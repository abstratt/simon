package com.abstratt.simon.compiler.source;

import java.io.StringReader;
import java.util.Map;
import java.util.Optional;

public class SimpleSourceProvider implements SourceProvider {
    private final Map<String, String> toParse;

    public SimpleSourceProvider(Map<String, String> toParse) {
        this.toParse = toParse;
    }

    @Override
    public ContentProvider access(String sourceName) {
        return Optional.ofNullable(toParse.get(sourceName)).map(StringReader::new).map(ContentProvider::provideContents)
                .orElse(null);
    }
}
