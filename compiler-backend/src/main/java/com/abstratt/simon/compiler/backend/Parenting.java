package com.abstratt.simon.compiler.backend;

import com.abstratt.simon.metamodel.Metamodel;

public interface Parenting<M, C extends Metamodel.Composition> {
    void addChild(C composition, M parent, M child);
}
