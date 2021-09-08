package com.abstratt.simon.compiler.backend;

import com.abstratt.simon.metamodel.Metamodel;

public interface Backend<O extends Metamodel.ObjectType, S extends Metamodel.Slotted, M> {
    NameSetting<M> nameSetting();

    NameQuerying<M> nameQuerying();

    NameResolution<M> nameResolution();

    <S1 extends S> Instantiation<S1> instantiation();

    <P extends Metamodel.Primitive> Declaration<P> declaration();

    <L extends Metamodel.Slot> ValueSetting<M, L> valueSetting();

    <F extends Metamodel.Reference> Linking<M, F> linking();

    <C extends Metamodel.Composition> Parenting<M, C> parenting();

    <R> R runOperation(Operation<R> operation);
    
    interface Factory {
    	<O extends Metamodel.ObjectType, S extends Metamodel.Slotted, M> Backend<O, S, M> create();
    }
}
