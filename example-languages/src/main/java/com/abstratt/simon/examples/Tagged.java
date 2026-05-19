package com.abstratt.simon.examples;

import java.util.Collection;

import com.abstratt.simon.metamodel.dsl.Meta;

/**
 * Small example metamodel exercising multivalued primitive slot values.
 */
@Meta.Package(builtIns = {})
public interface Tagged {

    @Meta.Composite(root = true)
    interface Item {
        @Meta.Name
        @Meta.Attribute
        String name();

        @Meta.Typed(String.class)
        @Meta.Attribute
        Collection<String> tags();
    }
}
