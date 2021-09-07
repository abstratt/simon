package com.abstratt.simon.compiler.source;

import com.abstratt.simon.metamodel.Metamodel.Type;
import java.util.stream.Stream;

/**
 * A type source provides access to the metamodel of the language.
 */
public interface MetamodelSource<T extends Type> extends AutoCloseable {

	/**
	 * Resolves the given type name into a type instance.
	 * 
	 * @param typeName
	 * @return the corresponding type instance
	 */
	T resolveType(String typeName);

	Stream<T> enumerate();

	SourceProvider builtInSources();

	default void close() {
	}

	interface Factory<T extends Type> {
		MetamodelSource<T> build();
	}

}
