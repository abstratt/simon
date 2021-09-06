package com.abstratt.simon.compiler.backend;

import com.abstratt.simon.metamodel.Metamodel;

public interface ValueSetting<M, S extends Metamodel.Slot> {
    <V> void setValue(S slot, M slotted, V value);
}
