package com.abstratt.simon.compiler.source;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Loads sources via {@link ClassLoader#getResourceAsStream(String)}, anchored
 * at a fixed classpath package path (e.g. {@code com/abstratt/simon/examples}).
 * Works regardless of whether the resource lives in a directory or a JAR —
 * unlike {@link URISourceProvider}, which relies on URI resolution that breaks
 * for opaque {@code jar:} URIs.
 */
public class ClasspathSourceProvider implements SourceProvider {
    private final ClassLoader classLoader;
    private final String basePath;
    private final String extension;

    public ClasspathSourceProvider(ClassLoader classLoader, String basePath, String extension) {
        this.classLoader = classLoader;
        this.basePath = basePath.endsWith("/") ? basePath : basePath + "/";
        this.extension = extension;
    }

    @Override
    public ContentProvider access(String sourceName) {
        String resourcePath = basePath + sourceName + "." + extension;
        return () -> {
            var stream = classLoader.getResourceAsStream(resourcePath);
            if (stream == null) {
                throw new java.io.FileNotFoundException(resourcePath);
            }
            return new InputStreamReader(stream, StandardCharsets.UTF_8);
        };
    }
}
