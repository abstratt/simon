package com.abstratt.simon.compiler.antlr;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.UnbufferedTokenStream;
import org.antlr.v4.runtime.tree.pattern.RuleTagToken;

import com.abstratt.simon.compiler.AbortCompilationException;
import com.abstratt.simon.compiler.SimonCompiler;
import com.abstratt.simon.compiler.Configuration;
import com.abstratt.simon.compiler.Problem;
import com.abstratt.simon.compiler.TypeSource;
import com.abstratt.simon.compiler.Configuration.Operation;
import com.abstratt.simon.compiler.Configuration.Provider;
import com.abstratt.simon.metamodel.Metamodel.ObjectType;
import com.abstratt.simon.metamodel.Metamodel.Slotted;
import com.abstratt.simon.parser.antlr.SimonLexer;
import com.abstratt.simon.parser.antlr.SimonParser;

public class SimonCompilerImpl<T> implements SimonCompiler<T>{
	public TypeSource<?> typeSource;

	public Configuration.Provider<? extends ObjectType, ? extends Slotted, T> configurationProvider;

	public SimonCompilerImpl(TypeSource<?> typeSource,
			Provider<? extends ObjectType, ? extends Slotted, T> configurationProvider) {
		this.typeSource = typeSource;
		this.configurationProvider = configurationProvider;
	}

	
	
//	/**
//	 * Compiles a project made of multiple compilation units.
//	 * 
//	 * Compilation units in a project may make references to one another.
//	 * 
//	 * @param cuPaths
//	 * @return
//	 */
//	public Result<T> compileProject(Path projectPath, Path... cuPaths) {
//		try {
//			return compile(CharStreams.fromPath(toParse));
//		} catch (IOException e) {
//			throw new CompilerException(e);
//		}
//	}

	@Override
	public List<Result<T>> compile(Stream<ContentProvider> inputs) {
		SimonBuilder<T> builder = new SimonBuilder<T>(typeSource, configurationProvider);
		return configurationProvider.runOperation(() -> doCompile(inputs, builder));
	}
	
	public List<Result<T>> doCompile(Stream<ContentProvider> inputs, SimonBuilder<T> builder) {
		return inputs.map(input -> { 
			try {
				parse(input.getContents(), builder);
			} catch (IOException e) {
				return new Result<T>(null, Arrays.asList(new Problem(-1, -1, e.toString())));
			} 
			return new Result<>(builder.build(), builder.getProblems());
		}).collect(Collectors.toList());
	}

	private void parse(Reader contents, SimonBuilder<T> builder) throws IOException {
		parse(CharStreams.fromReader(contents), builder);
	}

	private void parse(CharStream input, SimonBuilder<T> builder) {
		SimonLexer lexer = new SimonLexer(input);
		SimonParser parser = new SimonParser(new UnbufferedTokenStream<RuleTagToken>(lexer));
		parser.addParseListener(builder);
		parser.addErrorListener(new BaseErrorListener() {
			@Override
			public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
					int charPositionInLine, String msg, RecognitionException e) {
				builder.reportError(false, line, charPositionInLine, msg);
			}
		});
		try {
			parser.program();
			builder.checkAbort();
		} catch (AbortCompilationException e) {
			// a fatal compilation error
		}
	}
}
