package com.abstratt.simon.examples.kirra;

import java.util.Collection;
import java.util.List;

import org.apache.commons.text.WordUtils;

import com.abstratt.simon.metamodel.dsl.Meta;

@Meta.Package(builtIns = {"kirra"})
public interface Kirra {

	class Primitive implements BasicType {
		@Override
		public java.lang.String name() {
			return WordUtils.uncapitalize(getClass().getSimpleName());
		}
	}

	interface Named {
		@Meta.Name
		@Meta.Attribute
		java.lang.String name();
	}

	abstract class BaseNamed implements Named {
		private java.lang.String name;

		@Override
		public java.lang.String name() {
			return name;
		}
	}

	@Meta.Composite(root = true)
	class Namespace extends BaseNamed {
		private Collection<Entity> entities;
		private Collection<Primitive> primitives;

		@Meta.Contained
		@Meta.Typed(Entity.class)
		public Collection<Entity> entities() {
			return entities;
		}

		@Meta.Contained
		@Meta.Typed(Primitive.class)
		public Collection<Primitive> primitives() {
			return primitives;
		}
	}

	abstract class Feature<T extends Type> extends BaseTyped<T> {
		private boolean multiple;

		@Meta.Attribute
		public boolean multiple() {
			return multiple;
		}
	}

	class Property extends Feature<BasicType> {

	}

	class Relationship extends Feature<Entity> {
		private Relationship opposite;

		@Meta.Reference
		Relationship opposite() {
			return this.opposite;
		}
	}

	class Operation extends BaseNamed {

		private Collection<Parameter> parameters;
		private OperationKind kind;
		private boolean public_;

		enum OperationKind {
			Action, Constructor, Event, Finder, Retriever
		}

		@Meta.Contained
		Collection<Parameter> parameters() {
			return this.parameters;
		}

		@Meta.Attribute
		public OperationKind kind() {
			return kind;
		}

		@Meta.Attribute
		public boolean public_() {
			return public_;
		}
	}

	class Parameter extends BaseTyped<Type> {

	}

	interface TypedElement<T extends Type> {
		@Meta.Reference
		@Meta.Required
		T type();
	}

	abstract class BaseTyped<T extends Type> extends BaseNamed implements TypedElement<T> {
		protected T type;

		@Override
		public T type() {
			return type;
		}
	}

	class Entity extends BaseNamed implements Type {
		private Collection<Property> properties;
		private Collection<Relationship> relationships;
		private Collection<Operation> operations;
		private Collection<Entity> superTypes;
		private Collection<Entity> subTypes;
		private Namespace namespace;

		@Meta.Contained
		@Meta.Typed(Property.class)
		public Collection<Property> properties() {
			return properties;
		}

		@Meta.Reference(opposite = "subTypes")
		@Meta.Typed(Entity.class)
		public Collection<Entity> superTypes() {
			return superTypes;
		}

		@Meta.Reference
		@Meta.Typed(Entity.class)
		public Collection<Entity> subTypes() {
			return subTypes;
		}

		@Meta.Contained
		@Meta.Typed(Operation.class)
		public Collection<Operation> operations() {
			return operations;
		}

		@Meta.Contained
		@Meta.Typed(Relationship.class)
		public Collection<Relationship> relationships() {
			return relationships;
		}

		@Meta.Reference(opposite = "entities")
		public Namespace namespace() {
			return namespace;
		}
	}

	interface Type extends Named {

	}

	interface BasicType extends Type {

	}

	class TupleType extends BaseNamed implements BasicType {
		private List<BasicType> componentTypes;

		@Meta.Contained
		@Meta.Typed(BasicType.class)
		public List<BasicType> componentTypes() {
			return componentTypes;
		}
	}

}
