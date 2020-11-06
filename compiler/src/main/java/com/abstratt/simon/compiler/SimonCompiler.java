package com.abstratt.simon.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.UnbufferedTokenStream;
import org.antlr.v4.runtime.tree.pattern.RuleTagToken;

import com.abstratt.simon.compiler.Configuration.Provider;
import com.abstratt.simon.compiler.Metamodel.ObjectType;
import com.abstratt.simon.parser.antlr.SimonLexer;
import com.abstratt.simon.parser.antlr.SimonParser;

public class SimonCompiler<T> {
	private Metamodel metamodel;
	private TypeSource typeSource;
	private Configuration.Provider<? extends ObjectType,T> configurationProvider;
	public SimonCompiler(Metamodel metamodel, TypeSource typeSource, Provider<? extends ObjectType, T> configurationProvider) {
		this.metamodel = metamodel;
		this.typeSource = typeSource;
		this.configurationProvider = configurationProvider;
	}
	
	public T compile(URI uri) {
		try {
			return compile(uri.toURL());
		} catch (MalformedURLException e) {
			throw new CompilerException(e);
		}
	}
	
	public T compile(URL url) {
		try (InputStream contents = url.openStream()) {
			return compile(CharStreams.fromStream(contents));
		} catch (IOException e) {
			throw new CompilerException(e);
		}
	}	
	
	public T compile(Path toParse) {
		try {
			return compile(CharStreams.fromPath(toParse));
		} catch (IOException e) {
			throw new CompilerException(e);
		}
	}

	public T compile(String toParse) {
		try (StringReader reader = new StringReader(toParse)) {
			return compile(CharStreams.fromReader(reader));
		} catch (IOException e) {
		    throw new CompilerException(e);
		}
	}

	private T compile(CharStream input) {
		SimonLexer lexer = new SimonLexer(input);
		SimonParser parser = new SimonParser(new UnbufferedTokenStream<RuleTagToken>(lexer));
		SimonBuilder<T> builder = new SimonBuilder<T>(metamodel, typeSource, configurationProvider);
		parser.addParseListener(builder);
		parser.addErrorListener(new BaseErrorListener() {
			@Override
			public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
					int charPositionInLine, String msg, RecognitionException e) {
				builder.addProblem(new Problem(line, charPositionInLine, msg));
			}		
		});
		parser.program();
		if (builder.hasProblems()) {
			throw new CompilerException(builder.getProblems());
		}
		T result = builder.build();
		return result;
	}
}
