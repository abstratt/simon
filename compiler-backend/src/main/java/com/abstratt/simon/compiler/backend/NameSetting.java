package com.abstratt.simon.compiler.backend;

/**
 * Defines the contract for setting names of model elements.
 *
 * @param <M> the type of model element being named
 */
public interface NameSetting<M> {

    /**
     * Sets the name of the specified named object.
     *
     * @param named the named object
     * @param name  the new name
     */
    void setName(M named, String name);
}
