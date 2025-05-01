package com.abstratt.simon.compiler.backend;

/**
 * Defines the contract for querying names of model elements.
 *
 * @param <M> the type of model element being queried
 */
public interface NameQuerying<M> {

    /**
     * Obtains the name of the specified named object.
     *
     * @param named the named object
     * @return the name of the object
     */
    String getName(M named);
}
