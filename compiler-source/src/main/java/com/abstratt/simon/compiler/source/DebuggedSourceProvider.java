package com.abstratt.simon.compiler.source;

public class DebuggedSourceProvider implements SourceProvider {
    private final SourceProvider actual;

    public DebuggedSourceProvider(SourceProvider actual) {
        this.actual = actual;
    }

    @Override
    public ContentProvider access(String sourceName) {
        var contentProvider = actual.access(sourceName);
        return contentProvider;
    }

}
