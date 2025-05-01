package com.abstratt.simon.compiler.backend;

import com.abstratt.simon.metamodel.Metamodel;

/**
 * Manages parent-child relationships in model elements.
 *
 * @param <M> the type of model element being managed
 * @param <C> the type of composition being managed
 */
public interface Parenting<M, C extends Metamodel.Composition> {

    /**
     * Adds a child element to the specified parent element using the given composition.
     *
     * @param composition the composition defining the parent-child relationship
     * @param parent      the parent element
     * @param child       the child element to be added
     */
    void addChild(C composition, M parent, M child);
}
