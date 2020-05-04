package com.abstratt.simon.compiler;

import com.abstratt.simon.metamodel.Metamodel.Type;

public interface TypeSource<T extends Type> {

	/**
	 * Resolves the given type name into a type instance.
	 * 
	 * @param typeName
	 * @return the corresponding type instance
	 */
	T resolveType(String typeName);

	default void use(String packageName) {
		
	}
}
