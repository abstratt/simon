package com.abstratt.simon.examples;

import com.abstratt.simon.examples.IM.Entity;
import com.abstratt.simon.metamodel.dsl.Meta;

@Meta.Package(builtIns = {})
public interface DAUI {
	interface IEntityComponent extends UI.IComponent {
		@Meta.Reference
		@Meta.Typed(Entity.class)
		public Entity entity();
	}
	
	class EntityScreen extends UI.Screen implements IEntityComponent {
		private Entity entity;

		@Override
		public Entity entity() {
			return entity;
		}
		
	}
}
