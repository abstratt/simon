package com.abstratt.simon.compiler.backend;

import com.abstratt.simon.metamodel.Metamodel;

public interface Instantiation<S extends Metamodel.Slotted> {
    <OBJ> OBJ createObject(boolean root, S basicType);
}
