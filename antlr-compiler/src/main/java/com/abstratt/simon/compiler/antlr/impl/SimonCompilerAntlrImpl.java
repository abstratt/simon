package com.abstratt.simon.compiler.antlr.impl;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.UnbufferedTokenStream;
import org.antlr.v4.runtime.tree.pattern.RuleTagToken;
import org.apache.commons.lang3.tuple.Pair;

import com.abstratt.simon.compiler.AbortCompilationException;
import com.abstratt.simon.compiler.Problem;
import com.abstratt.simon.compiler.Problem.Severity;
import com.abstratt.simon.compiler.Result;
import com.abstratt.simon.compiler.SimonCompiler;
import com.abstratt.simon.compiler.backend.Backend;
import com.abstratt.simon.compiler.source.ContentProvider;
import com.abstratt.simon.compiler.source.DebuggedSourceProvider;
import com.abstratt.simon.compiler.source.MetamodelSource;
import com.abstratt.simon.compiler.source.SourceProvider;
import com.abstratt.simon.compiler.source.SourceProviderChain;
import com.abstratt.simon.metamodel.Metamodel.ObjectType;
import com.abstratt.simon.metamodel.Metamodel.Slotted;
import com.abstratt.simon.parser.antlr.SimonLexer;
import com.abstratt.simon.parser.antlr.SimonParser;

public class SimonCompilerAntlrImpl<T> implements SimonCompiler<T>{
	public final MetamodelSource.Factory<?> typeSourceFactory;

	public final Backend<? extends ObjectType, ? extends Slotted, T> modelHandling;

	public SimonCompilerAntlrImpl(MetamodelSource.Factory<?> typeSourceFactory,
			Backend<? extends ObjectType, ? extends Slotted, T> configurationProvider) {
		this.typeSourceFactory = typeSourceFactory;
		this.modelHandling = configurationProvider;
	}


	@Override
	public List<Result<T>> compile(List<String> entryPoints, SourceProvider sources) {
		try (var typeSource = typeSourceFactory.build()) {
			var builtInSources = typeSource.builtInSources();
			var augmentedSources = new SourceProviderChain(Arrays.asList(new DebuggedSourceProvider(builtInSources), new DebuggedSourceProvider(sources)));
			return doCompile(entryPoints, augmentedSources, typeSource);
		}

	}

	private ArrayList<Result<T>> doCompile(List<String> entryPoints, SourceProvider sources, MetamodelSource<?> typeSource) {
		var problemHandler = new ProblemHandler();
		var builder = new SimonBuilder<>(problemHandler, typeSource, modelHandling);
		var results = modelHandling.runOperation(() -> parseUnits(sources, entryPoints, builder));
		builder.resolve();
		problemHandler.getAllProblems().forEach((source, problem) -> {
			Result<T> sourceResult = results.computeIfAbsent(source, s -> Result.failure(s, new Problem(source, "Missing source", Severity.Fatal)));
			sourceResult.getProblems().addAll(problem);
		});
		return new ArrayList<>(results.values());
	}

	private Map<String, Result<T>> parseUnits(SourceProvider sources, List<String> entryPoints, SimonBuilder<T> builder) {
		Set<String> toParse = new LinkedHashSet<>(entryPoints);
		Set<String> alreadyParsed = new LinkedHashSet<>();
		var results = new LinkedHashMap<String, Result<T>>();

		while (!toParse.isEmpty()) {
			var thisBatch = toParse;
			var contentProviders = thisBatch.stream().map(it -> Pair.of(it, sources.access(it)));
			var batchResults = contentProviders.map(input -> parseUnit(builder, input.getKey(), input.getValue()));
			batchResults.forEach(it -> results.put(it.getSource(), it));
			alreadyParsed.addAll(thisBatch);
			List<String> imports = builder.collectImports();
			toParse = new LinkedHashSet<>(imports);
			toParse.removeIf(alreadyParsed::contains);
		}
		return results;
	}

	private Result<T> parseUnit(SimonBuilder<T> builder, String name, ContentProvider input) {
		if (input == null) {
			return Result.failure(name, new Problem(name, "No source found for '" + name + "'", Problem.Severity.Fatal));
		}
		try {
			doParse(name, input.getContents(), builder);
		} catch (AbortCompilationException e) {
			// aborted due to fatal error
		} catch (IOException e) {
			return Result.failure(name, new Problem(name, e.toString(), Problem.Severity.Fatal));
		}
		var roots = builder.buildUnit();
		return new Result(name, roots, Collections.emptyList());
	}

	private void doParse(String source, Reader contents, SimonBuilder<T> builder) throws IOException {
		doParse(source, CharStreams.fromReader(contents), builder);
	}

	private void doParse(String source, CharStream input, SimonBuilder<T> builder) {
		SimonLexer lexer = new SimonLexer(input);
		SimonParser parser = new SimonParser(new UnbufferedTokenStream<RuleTagToken>(lexer));
		parser.addParseListener(builder);
		parser.addErrorListener(new BaseErrorListener() {
			@Override
			public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
					int charPositionInLine, String msg, RecognitionException e) {
				builder.reportError(Problem.Severity.Error, source, line, charPositionInLine, msg);
			}
		});
		builder.startSource(source);
		try {
			parser.program();
		} finally {
			builder.endSource(source);
		}
	}
}


class ProblemHandler implements Problem.Handler {
	private final Map<String, List<Problem>> problems = new LinkedHashMap<>();
	private boolean hasFatalProblem = false;


	public Map<String, List<Problem>> getAllProblems() {
		return problems;
	}
	public List<Problem> getProblems(String source) {
		return problems.getOrDefault(source, Collections.emptyList());
	}

	@Override
	public void handleProblem(Problem toHandle) {
		String source = toHandle.source();
		List<Problem> sourceProblems = this.problems.computeIfAbsent(source, it -> new ArrayList<>());
		sourceProblems.add(toHandle);
		hasFatalProblem = hasFatalProblem || toHandle.category() == Problem.Severity.Fatal;
	}
}