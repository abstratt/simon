package com.abstratt.simon.compiler.backend;

public interface NameQuerying<M> {
    /**
     * Being M a 'nameable' object, this method obtains M's name.
     *
     * @param named the named object
     * @returns the object new name
     */
    String getName(M named);
}
