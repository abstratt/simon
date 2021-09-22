package com.abstratt.simon.compiler.backend;

public interface NameResolution<M> {
    /**
     * Starting from the given scope object, resolves the given name to an object.
     *
     * @param scope
     * @param path
     * @return the resolved object
     */
    M resolve(M scope, String... path);
}
