package com.abstratt.simon.compiler.source;

import java.util.Set;
import java.util.stream.Stream;

import com.abstratt.simon.metamodel.Metamodel.Type;

/**
 * A type source provides access to the metamodel of the language.
 */
public interface MetamodelSource<T extends Type> extends AutoCloseable {

    /**
     * Resolves the given type name into a type instance.
     * 
     * @param typeName
     * @param languages, or null
     * @return the corresponding type instance
     */
    T resolveType(String typeName, Set<String> languages);

    /**
     * Enumerates all types available in the given languages.
     *
     * @param languages the languages to enumerate types from, or null for all
     * @return the available types
     */
    Stream<T> enumerate(Set<String> languages);

    /**
     * Source text for the metamodel's built-in definitions (such as primitive
     * types) that should be available to every compilation against this metamodel.
     */
    SourceProvider builtInSources();

    default void close() {
    }

    interface Factory<T extends Type> {
        MetamodelSource<T> build();
    }

}
