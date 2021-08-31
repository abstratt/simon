package com.abstratt.simon.compiler;

import java.io.StringReader;
import java.util.Map;
import java.util.Optional;

public class ContentProviderSourceProvider implements SimonCompiler.SourceProvider {
	private final Map<String, SimonCompiler.ContentProvider> toParse;
	ContentProviderSourceProvider(Map<String, SimonCompiler.ContentProvider> toParse) {
		this.toParse = toParse;
	}

	@Override
	public SimonCompiler.ContentProvider access(String sourceName) {
		return Optional.ofNullable(toParse.get(sourceName))
				.orElse(null);
	}
}
