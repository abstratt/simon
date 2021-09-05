package com.abstratt.simon.compiler.source;

import java.util.Map;
import java.util.Optional;

public class ContentProviderSourceProvider implements SourceProvider {
	private final Map<String, ContentProvider> toParse;
	public ContentProviderSourceProvider(Map<String, ContentProvider> toParse) {
		this.toParse = toParse;
	}

	@Override
	public ContentProvider access(String sourceName) {
		return Optional.ofNullable(toParse.get(sourceName))
				.orElse(null);
	}
}
