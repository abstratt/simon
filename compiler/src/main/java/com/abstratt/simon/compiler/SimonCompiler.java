package com.abstratt.simon.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.UnbufferedTokenStream;
import org.antlr.v4.runtime.tree.pattern.RuleTagToken;

import com.abstratt.simon.compiler.Configuration.Provider;
import com.abstratt.simon.metamodel.Metamodel.ObjectType;
import com.abstratt.simon.metamodel.Metamodel.Slotted;
import com.abstratt.simon.parser.antlr.SimonLexer;
import com.abstratt.simon.parser.antlr.SimonParser;

public class SimonCompiler<T> {
	private TypeSource<?> typeSource;

	private Configuration.Provider<? extends ObjectType, ? extends Slotted, T> configurationProvider;

	interface ContentProvider {
		CharStream getContents() throws IOException;
	}
	
	public static class Result<T> {
		private final T rootObject;
		private final List<Problem> problems;

		public Result(T rootObject, List<Problem> problems) {
			this.rootObject = rootObject;
			this.problems = problems;
		}

		public T getRootObject() {
			return rootObject;
		}

		public List<Problem> getProblems() {
			return problems;
		}
	}

	public SimonCompiler(TypeSource<?> typeSource,
			Provider<? extends ObjectType, ? extends Slotted, T> configurationProvider) {
		this.typeSource = typeSource;
		this.configurationProvider = configurationProvider;
	}

	public Result<T> compile(URI uri) {
		try {
			return compile(uri.toURL());
		} catch (MalformedURLException e) {
			throw new CompilerException(e);
		}
	}

	public Result<T> compile(URL url) {
		try (InputStream contents = url.openStream()) {
			return compile(() -> CharStreams.fromStream(contents));
		} catch (IOException e) {
			throw new CompilerException(e);
		}
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


	public Result<T> compile(Path toParse) {
		return compile(() -> CharStreams.fromPath(toParse));
	}

	public Result<T> compile(String toParse) {
		return compile(() -> CharStreams.fromReader(new StringReader(toParse)));
	}
	
	public List<Result<T>> compile(String... toParse) {
		Stream<String> stream = Arrays.stream(toParse);
		Stream<StringReader> readers = stream.map(StringReader::new);
		Stream<ContentProvider> contentProviders = readers.map(it -> () -> CharStreams.fromReader(it));
		return compile(contentProviders);
	}
	
	private Result<T> compile(ContentProvider input) {
		return compile(Stream.of(input)).get(0);
	}
	
	public List<Result<T>> compile(Stream<ContentProvider> inputs) {
		SimonBuilder<T> builder = new SimonBuilder<T>(typeSource, configurationProvider);
		return configurationProvider.runOperation(new Configuration.Operation<List<Result<T>>>() {
			@Override
			public List<Result<T>> run() {
				Stream<Result<T>> compiled = doCompile(inputs, builder);
				return compiled.collect(Collectors.toList());
			}
		});
	}
	

	private Stream<Result<T>> doCompile(Stream<ContentProvider> inputs, SimonBuilder<T> builder) {
		return inputs.map(input -> { 
			try {
				parse(input.getContents(), builder);
			} catch (IOException e) {
				return new Result<T>(null, Arrays.asList(new Problem(-1, -1, e.toString())));
			} 
			return new Result<>(builder.build(), builder.getProblems());
		});
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
