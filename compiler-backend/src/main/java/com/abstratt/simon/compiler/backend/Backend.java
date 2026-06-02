package com.abstratt.simon.compiler.backend;

import com.abstratt.simon.metamodel.Metamodel;

/**
 * Builds the model as a Simon source is compiled.
 *
 * A backend is a factory of small operation handlers, each performing one
 * model-construction task the compiler requests while walking the parse tree
 * (instantiating elements, setting slot values, attaching children, linking
 * references, and so on). Implementing this interface is how Simon targets a
 * new kind of model output.
 *
 * @param <O> the kind of object type this backend can instantiate
 * @param <S> the kind of slotted type (object or record) this backend handles
 * @param <M> the type of model object produced
 */
public interface Backend<O extends Metamodel.ObjectType, S extends Metamodel.Slotted, M> {
    /** Sets the name of a model object. */
    NameSetting<M> nameSetting();

    /**
     * Attaches a documentation comment (from a {@code (* ... *)} model comment) to
     * a model object.
     */
    Documenting<M> documenting();

    /** Reads the name of a model object. */
    NameQuerying<M> nameQuerying();

    /**
     * Resolves a name path to a model object, used to wire references during the
     * resolution pass.
     */
    NameResolution<M> nameResolution();

    /**
     * Creates a model object for a declared element. The {@code root} flag marks
     * top-level elements.
     */
    <S1 extends S> Instantiation<S1> instantiation();

    /** Declares a primitive value. */
    <P extends Metamodel.Primitive> Declaration<P> declaration();

    /**
     * Sets the value of a slot (a primitive, enumerated, or record value) on a
     * model object.
     */
    <L extends Metamodel.Slot> ValueSetting<M, L> valueSetting();

    /**
     * Wires up a reference between model objects. Performed during the resolution
     * pass, once all elements have been instantiated.
     */
    <F extends Metamodel.Reference> Linking<M, F> linking();

    /** Attaches an owned child to its parent via a composition. */
    <C extends Metamodel.Composition> Parenting<M, C> parenting();

    /**
     * Runs a unit of work in the backend's execution context (for example, within
     * a transaction).
     */
    <R> R runOperation(Operation<R> operation);

    interface Factory {
        /** Creates a new backend instance. */
        <O extends Metamodel.ObjectType, S extends Metamodel.Slotted, M> Backend<O, S, M> create();
    }
}
