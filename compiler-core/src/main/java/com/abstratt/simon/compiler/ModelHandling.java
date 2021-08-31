package com.abstratt.simon.compiler;

import com.abstratt.simon.genutils.Traversal;
import com.abstratt.simon.metamodel.Metamodel.Composition;
import com.abstratt.simon.metamodel.Metamodel.ObjectType;
import com.abstratt.simon.metamodel.Metamodel.Reference;
import com.abstratt.simon.metamodel.Metamodel.Slot;
import com.abstratt.simon.metamodel.Metamodel.Slotted;
import com.abstratt.simon.metamodel.Metamodel.Primitive;

/**
 * A model handling implementation knows how to build model elements, and connect them to form models. 
 */
public interface ModelHandling {
	interface Operation<R> {
		R run();
	}
	
	interface Provider<O extends ObjectType, S extends Slotted, M> {
		NameSetting<M> nameSetting();
		NameQuerying<M> nameQuerying();
		NameResolution<M> nameResolution();
		<S1 extends S> Instantiation<S1> instantiation();
		<P extends Primitive> Declaration<P> declaration();
		<L extends Slot> ValueSetting<M, L> valueSetting();
		<F extends Reference> Linking<M, F> linking();
		<C extends Composition> Parenting<M, C> parenting();
		<R> R runOperation(Operation<R> operation);
	}

	interface Instantiation<S extends Slotted> {
		<OBJ> OBJ createObject(boolean root, S basicType);
	}
	
	interface Declaration<P extends Primitive> {
		<OBJ> OBJ declarePrimitive(P primitiveType);
	}
	
	interface NameSetting<M> {
		/**
		 * Being M a 'nameable' object, this method sets the object name.
		 * 
		 * @param named the named object
		 * @param name  the new name
		 */
		void setName(M named, String name);
	}

	interface NameQuerying<M> {
		/**
		 * Being M a 'nameable' object, this method obtains M's name.
		 * 
		 * @param named the named object
		 * @returns the object new name
		 */
		String getName(M named);
	}

	interface NameResolution<M> {
		/**
		 * Starting from the given scope object, resolves the given name 
		 * to an object.
		 * 
		 * @param scope
		 * @param path
		 * @return the resolved object
		 */
		M resolve(M scope, String... path);
	}

	interface ValueSetting<M, S extends Slot> {
		<V> void setValue(S slot, M slotted, V value);
	}

	interface Linking<M, R extends Reference> {
		/**
		 * An object needs to reference another object.
		 *
		 * @param reference
		 * @param referrer
		 * @param referred
		 */
		void link(R reference, M referrer, M referred);
	}

	interface Parenting<M, C extends Composition> {
		void addChild(C composition, M parent, M child);
	}
}