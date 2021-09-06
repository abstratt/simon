package com.abstratt.simon.compiler.backend;

import com.abstratt.simon.metamodel.Metamodel;

public interface Declaration<P extends Metamodel.Primitive> {
    <OBJ> OBJ declarePrimitive(P primitiveType);
}
