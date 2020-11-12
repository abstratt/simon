package com.abstratt.simon.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.UnbufferedTokenStream;
import org.antlr.v4.runtime.tree.pattern.RuleTagToken;
import org.eclipse.emf.ecore.EObject;

import com.abstratt.simon.compiler.Configuration.Provider;
import com.abstratt.simon.compiler.Metamodel.ObjectType;
import com.abstratt.simon.compiler.Metamodel.Slotted;
import com.abstratt.simon.parser.antlr.SimonLexer;
import com.abstratt.simon.parser.antlr.SimonParser;

public class SimonCompiler<T> {
	private TypeSource<?> typeSource;
	private Configuration.Provider<? extends ObjectType,? extends Slotted, T> configurationProvider;
	
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
	
	public SimonCompiler(TypeSource<?> typeSource, Provider<? extends ObjectType,? extends Slotted, T> configurationProvider) {
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
			return compile(CharStreams.fromStream(contents));
		} catch (IOException e) {
			throw new CompilerException(e);
		}
	}	
	
	public Result<T> compile(Path toParse) {
		try {
			return compile(CharStreams.fromPath(toParse));
		} catch (IOException e) {
			throw new CompilerException(e);
		}
	}

	public Result<T> compile(String toParse) {
		try (StringReader reader = new StringReader(toParse)) {
			return compile(CharStreams.fromReader(reader));
		} catch (IOException e) {
		    throw new CompilerException(e);
		}
	}

	private Result<T> compile(CharStream input) {
		SimonLexer lexer = new SimonLexer(input);
		SimonParser parser = new SimonParser(new UnbufferedTokenStream<RuleTagToken>(lexer));
		SimonBuilder<T> builder = new SimonBuilder<T>(typeSource, configurationProvider);
		parser.addParseListener(builder);
		parser.addErrorListener(new BaseErrorListener() {
			@Override
			public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
					int charPositionInLine, String msg, RecognitionException e) {
				builder.reportError(false, line, charPositionInLine, msg);
			}		
		});
		T result = null;
		try {
			parser.program();
			builder.checkAbort();
			result = builder.build();
		} catch (AbortCompilationException e) {
			// a fatal compilation error
		}
		return new Result<>(result, builder.getProblems());
	}
}
