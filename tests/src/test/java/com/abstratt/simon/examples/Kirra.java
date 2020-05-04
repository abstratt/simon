package com.abstratt.simon.examples;

import java.util.Collection;
import java.util.List;

import com.abstratt.simon.Meta;

@Meta.Package
public interface Kirra {

	public static interface Named {
		@Meta.Name
		String name();
	}
	
	static abstract class BaseNamed implements Named {
		private String name;

		@Override
		public String name() {
			return name;
		}
	}

	class Namespace extends BaseNamed {
		private Collection<Entity> entities;

		@Meta.Contained
		@Meta.Typed(Entity.class)
		public Collection<Entity> entities() {
			return entities;
		}

	}

	class Property extends BaseTyped<BasicType> {

	}


	class Relationship extends BaseTyped<Entity> {

	}
	
	interface TypedElement<T extends Type> {
	    T type();
	}
	
	abstract class BaseTyped<T extends Type> extends BaseNamed implements TypedElement<T> {
		protected T type;
		@Override
		public T type() {
			return type;
		}
	}

	class EntityRef {
		String entityName;
	}

	class Entity extends BaseNamed implements Type {
		private Collection<Property> properties;
		private Collection<Relationship> relationships;
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
		@Meta.Typed(Relationship.class)
		public Collection<Relationship> relationships() {
			return relationships;
		}
		
		@Meta.Reference(opposite="entities")
		public Namespace namespace() {
			return namespace;
		}
	}

	interface Type extends Named {

	}

	interface BasicType extends Type {

	}

	enum Primitive implements BasicType {

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
