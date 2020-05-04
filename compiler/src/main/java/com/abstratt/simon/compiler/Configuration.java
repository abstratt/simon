package com.abstratt.simon.compiler;

import com.abstratt.simon.compiler.Metamodel.Composition;
import com.abstratt.simon.compiler.Metamodel.ObjectType;
import com.abstratt.simon.compiler.Metamodel.Reference;
import com.abstratt.simon.compiler.Metamodel.Slot;

public interface Configuration {
	interface Provider<T extends ObjectType, M> {
		Naming<M> naming();
		Instantiation<T> instantiation();
		<S extends Slot> ValueSetting<M, S> valueSetting(); 
		<R extends Reference> Linking<M, R> linking();
		<C extends Composition> Parenting<M, C> parenting();
	}
	
	interface Instantiation<T extends ObjectType> {
		<M> M createObject(T resolvedType);
	}
	
	interface Naming<M> {
		/**
		 * Being M a 'nameable' object, this method sets the object name.
		 * 
		 * @param named the named object
		 * @param name the new name
		 */
		void setName(M named, String name);
	}
	
	interface ValueSetting<M, S extends Slot> {
		<V> void setValue(S slot, M slotted, V value);
	}
	
	interface Linking<M, R extends Reference> {
		/**
		 * An object needs to reference another object.
		 * 
		 * @param sourceObject
		 * @param targetObject
		 */
		void link(R reference, M referrer, M referred);
	}
	
	interface Parenting<M, C extends Composition> {
		void addChild(C composition, M parent, M child);
	}
}