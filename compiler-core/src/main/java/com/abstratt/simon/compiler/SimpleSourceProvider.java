package com.abstratt.simon.compiler;

import java.io.StringReader;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class SimpleSourceProvider implements SimonCompiler.SourceProvider {
	private Map<String, String> toParse;
	public SimpleSourceProvider(Map<String, String> toParse) {
		this.toParse = toParse;
	}

	@Override
	public SimonCompiler.ContentProvider access(String sourceName) {
		return Optional.ofNullable(toParse.get(sourceName))
				.map(StringReader::new)
				.map(SimonCompiler.ContentProvider::provideContents)
				.orElse(null);
	}
}
