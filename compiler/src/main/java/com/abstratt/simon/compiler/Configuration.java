package com.abstratt.simon.compiler;

import com.abstratt.simon.compiler.Metamodel.Composition;
import com.abstratt.simon.compiler.Metamodel.ObjectType;
import com.abstratt.simon.compiler.Metamodel.Reference;
import com.abstratt.simon.compiler.Metamodel.Slot;
import com.abstratt.simon.compiler.Metamodel.Slotted;

public interface Configuration {
	interface Provider<O extends ObjectType, S extends Slotted, M> {
		NameSetting<M> nameSetting();
		NameQuerying<M> nameQuerying();
		NameResolution<M> nameResolution();
		<S1 extends S> Instantiation<S1> instantiation();
		<L extends Slot> ValueSetting<M, L> valueSetting(); 
		<F extends Reference> Linking<M, F> linking();
		<C extends Composition> Parenting<M, C> parenting();
	}
	
	interface Instantiation<S extends Slotted> {
		<OBJ> OBJ createObject(S basicType);
	}
	
	interface NameSetting<M> {
		/**
		 * Being M a 'nameable' object, this method sets the object name.
		 * 
		 * @param named the named object
		 * @param name the new name
		 */
		void setName(M named, String name);
	}
	interface NameQuerying<M> {
		/**
		 * Being M a 'nameable' object, this method sets the object name.
		 * 
		 * @param named the named object
		 * @param name the new name
		 */
		String getName(M named);
	}
	
	interface NameResolution<M> {
		M resolve(M scope, String... path);
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