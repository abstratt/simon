package com.abstratt.simon.metamodel;

import java.util.Collection;

/**
 * Describes a metamodel.
 * 
 * A metamodel is a set of types. Types may be:
 * <ul>
 * <li>values or objects: objects have identity, can have relationships. 
 * Values have no identity: primitive values, enumerated values, records</li>
 * <li>atomic or structured: an atomic type has no parts, a structured type (objects, records) is made of parts</li>
 * </ul>
 */
public interface Metamodel {

	/**
	 * If some model element can be named, should implement Named. 
	 */
	interface Named {
		String name();
		static boolean isNamed(Object o) {
			return o instanceof Named;
		}
	}

	interface Type extends Named {
		/**
		 * Is this the type for an a top-level element (which does not require a type)?
		 */
		boolean isRoot();

	}
	
	/**
	 * An interface for data elements that can have a type.
	 */
	interface Typed<T extends Type> extends Named {
		/**
		 * Is this piece of data required?
		 */
		boolean required();

		/**
		 * Can this piece of data admit multiple values?
		 */
		boolean multivalued();

		/**
		 * The type.
		 */
		T type();
	}	

	interface Slotted extends Type {
		Collection<Slot> slots();
		default Slot slotByName(String name) {
			return slots().stream().filter(s -> name.equals(s.name())).findAny().orElse(null);
		}
	}

	interface ObjectType extends Slotted {
		Collection<Composition> compositions();
		Collection<Reference> references();
		default Composition compositionByName(String name) {
			return compositions().stream().filter(s -> name.equals(s.name())).findAny().orElse(null);
		}
		default Reference referenceByName(String name) {
			return references().stream().filter(s -> name.equals(s.name())).findAny().orElse(null);
		}
	}

	interface RecordType extends Slotted, BasicType {
	}

	interface BasicType extends Type {
	}

	interface Enumerated extends BasicType {
		Object valueForName(String valueName);
	}

	interface Primitive extends BasicType {
		PrimitiveKind kind();
	}
	
	enum PrimitiveKind {
		Integer,
		Decimal,
		Boolean,
		String,
		Other
	}

	
	interface Feature<T extends Type> extends Typed<T> {
		
	}

	interface Slot extends Feature<BasicType> {

	}

	interface Composition extends Feature<ObjectType> {

	}

	interface Reference extends Feature<ObjectType> {

	}
}
