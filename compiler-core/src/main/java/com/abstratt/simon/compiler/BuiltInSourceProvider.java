package com.abstratt.simon.compiler;

import java.io.InputStreamReader;

public class BuiltInSourceProvider implements SimonCompiler.SourceProvider {
    private final Class<?> referenceClass;

    public BuiltInSourceProvider(Class<?> referenceClass) {
        this.referenceClass = referenceClass;
    }

    @Override
    public SimonCompiler.ContentProvider access(String sourceName) {
        return () -> {
            var url = referenceClass.getResource(sourceName);
            return url == null ? null : new InputStreamReader(url.openStream());
        };
    }
}
