package com.abstratt.simon.compiler.source;

public interface SourceProvider {
    SourceProvider NULL = source -> null;

    /**
     * Requests access to a source with the given name.
     *
     * @param sourceName
     * @return a content provider for accessing such source, or null if one could
     *         not be found
     */
    ContentProvider access(String sourceName);
}
