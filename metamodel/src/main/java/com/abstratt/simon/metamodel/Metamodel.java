package com.abstratt.simon.metamodel;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Describes a metamodel.
 * 
 * A metamodel is a set of types. Types may be:
 * <ul>
 * <li>values or objects: objects have identity, can have relationships. Values
 * have no identity: primitive values, enumerated values, records</li>
 * <li>atomic or structured: an atomic type has no parts, a structured type
 * (objects, records) is made of parts</li>
 * </ul>
 * 
 * @see Metamodel.Slotted
 * @see Metamodel.Feature
 */
public interface Metamodel {

	/**
	 * Model elements that may be named, should implement Named.
	 */
	interface Named {
		String name();

		static boolean isNamed(Object o) {
			return o instanceof Named;
		}
	}

	/**
	 * The basic interface for all types.
	 * 
	 * @see Metamodel.ObjectType
	 * @see Metamodel.RecordType
	 * @see Metamodel.Enumerated
	 * @see Metamodel.Primitive
	 */
	interface Type extends Named {
		/**
		 * Whether this type is meant for a top-level element (which does not require a
		 * parent type).
		 */
		boolean isRoot();

		boolean isInstantiable();
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

	/**
	 * A type for values that are made up of slots, such as {@link RecordType} and
	 * {@link ObjectType}.
	 */
	interface Slotted extends Type {
		Collection<Slot> slots();

		default Slot slotByName(String name) {
			return slots().stream().filter(s -> name.equals(s.name())).findAny().orElse(null);
		}
	}

	/** A trait for types that are made up of {@link Feature}s. */
	interface Featured extends Type {
		Collection<Feature> features();

		default Feature featureByName(String name) {
			return features().stream().filter(s -> name.equals(s.name())).findAny().orElse(null);
		}
	}

	/**
	 * An object is a model element that can contain others, and contain reference
	 * to others.
	 */
	interface ObjectType extends Slotted, Featured {
		Collection<Composition> compositions();

		Collection<Reference> references();

		default Composition compositionByName(String name) {
			return compositions().stream().filter(s -> name.equals(s.name())).findAny().orElse(null);
		}

		default Reference referenceByName(String name) {
			return references().stream().filter(s -> name.equals(s.name())).findAny().orElse(null);
		}
	}

	/** A record is a model element that can contain basic values. */
	interface RecordType extends Slotted, BasicType, Featured {
		default Collection<Feature> features() {
			return new ArrayList<>(slots());
		}
	}

	/**
	 * Super type for basic types, such as {@link Enumerated}, {@link Primitive} or
	 * {@link RecordType}.
	 */
	interface BasicType extends Type {
	}

	/**
	 * A type for enumerated values.
	 *
	 */
	interface Enumerated extends BasicType {
		Object valueForName(String valueName);
	}

	/**
	 * Primitive types are basic types that are not slotted (like
	 * {@link RecordType}) or {@link Enumerated}.
	 * 
	 * Metamodels may support many primitive types, but they must be of one of the
	 * kinds described by {@link PrimitiveKind}.
	 *
	 */
	interface Primitive extends BasicType {
		PrimitiveKind kind();
	}

	/**
	 * Metamodels may support many primitive types, but they must be of one of the
	 * kinds described here.
	 */
	enum PrimitiveKind {
		Integer, Decimal, Boolean, String, Other
	}

	/**
	 * Some types support features.
	 */
	interface Feature<T extends Type> extends Typed<T> {

	}

	/**
	 * A slot is a feature that can hold a basic value.
	 */
	interface Slot extends Feature<BasicType> {

	}

	/**
	 * A feature that depicts an owned object.
	 *
	 */
	interface Composition extends Feature<ObjectType> {

	}

	/**
	 * A feature that depicts a reference to another (non-owned) object.
	 */
	interface Reference extends Feature<ObjectType> {

	}
}
