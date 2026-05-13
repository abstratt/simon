package com.abstratt.simon.compiler.backend;

public interface Documenting<M> {
    void document(M toBeDocumented, String comment);
}
