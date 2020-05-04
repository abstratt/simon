package com.abstratt.simon.compiler;

import java.io.IOException;
import java.io.StringReader;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.UnbufferedTokenStream;
import org.antlr.v4.runtime.tree.pattern.RuleTagToken;

import com.abstratt.simon.parser.antlr.SimonLexer;
import com.abstratt.simon.parser.antlr.SimonParser;

public class SimonCompiler<T> {
	private Metamodel metamodel;
	private Configuration.Provider configurationProvider;
	public SimonCompiler(Metamodel metamodel, Configuration.Provider<?, ?> configurationProvider) {
		this.metamodel = metamodel;
		this.configurationProvider = configurationProvider;
	}

	public T compile(String toParse) {
		try {
			CharStream input = CharStreams.fromReader(new StringReader(toParse));
			SimonLexer lexer = new SimonLexer(input);
			SimonParser parser = new SimonParser(new UnbufferedTokenStream<RuleTagToken>(lexer));
			SimonBuilder<T> builder = new SimonBuilder<T>(metamodel, configurationProvider);
			parser.addParseListener(builder);
			parser.rootObject();
			return builder.build();
		} catch (IOException e) {
			throw new CompilerException(e);
		}
	}
}
