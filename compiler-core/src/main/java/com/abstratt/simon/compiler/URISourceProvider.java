package com.abstratt.simon.compiler;

import java.io.InputStreamReader;
import java.net.URI;
import java.util.Optional;

public class URISourceProvider implements SimonCompiler.SourceProvider {
    private final URI baseURI;
    private final String extension;

    public URISourceProvider(URI baseURI, String extension) {
        this.baseURI = baseURI;
        this.extension = extension;
    }

    @Override
    public SimonCompiler.ContentProvider access(String sourceName) {
        var sourceURI = baseURI.resolve(sourceName + Optional.ofNullable(extension).map(it -> "." + it).orElse(""));
        return () -> new InputStreamReader(sourceURI.toURL().openStream());
    }
}
