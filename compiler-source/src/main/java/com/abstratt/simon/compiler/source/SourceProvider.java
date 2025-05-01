package com.abstratt.simon.compiler.source;

/**
 * Defines the contract for accessing sources in the Simon compiler.
 * Provides a method to request access to a source with a given name and returns a ContentProvider for accessing the source.
 */
public interface SourceProvider {
    SourceProvider NULL = source -> null;

    /**
     * Requests access to a source with the given name.
     *
     * @param sourceName the name of the source to access
     * @return a content provider for accessing the source, or null if one could not be found
     */
    ContentProvider access(String sourceName);
}
