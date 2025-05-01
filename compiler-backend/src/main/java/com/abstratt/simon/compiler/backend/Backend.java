package com.abstratt.simon.compiler.backend;

import com.abstratt.simon.metamodel.Metamodel;

/**
 * Defines the contract for various backend operations in the Simon compiler.
 *
 * @param <O> the type of object being handled
 * @param <S> the type of slotted element being handled
 * @param <M> the type of model element being handled
 */
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

    /**
     * Factory interface for creating Backend instances.
     */
    interface Factory {
        <O extends Metamodel.ObjectType, S extends Metamodel.Slotted, M> Backend<O, S, M> create();
    }
}
