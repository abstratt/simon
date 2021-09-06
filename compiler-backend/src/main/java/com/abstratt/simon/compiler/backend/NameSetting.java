package com.abstratt.simon.compiler.backend;

public interface NameSetting<M> {
    /**
     * Being M a 'nameable' object, this method sets the object name.
     *
     * @param named the named object
     * @param name  the new name
     */
    void setName(M named, String name);
}
