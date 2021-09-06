package com.abstratt.simon.compiler.backend;

import com.abstratt.simon.metamodel.Metamodel;

public interface Linking<M, R extends Metamodel.Reference> {
    /**
     * An object needs to reference another object.
     *
     * @param reference
     * @param referrer
     * @param referred
     */
    void link(R reference, M referrer, M referred);
}
