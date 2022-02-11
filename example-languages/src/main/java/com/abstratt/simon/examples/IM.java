package com.abstratt.simon.examples;

import java.util.Collection;
import java.util.List;

import org.apache.commons.text.WordUtils;

import com.abstratt.simon.metamodel.dsl.Meta;

@Meta.Package(builtIns = { "im" })
public interface IM {

    interface Primitive extends BasicType {
        @Override
        default String name() {
            return WordUtils.uncapitalize(getClass().getSimpleName());
        }
    }

    interface Named {
        @Meta.Name
        @Meta.Attribute
        String name();
    }

    @Meta.Composite(root = true)
    interface Namespace extends Named {
        @Meta.Contained
        @Meta.Typed(Entity.class)
        Collection<Entity> entities();

        @Meta.Contained
        @Meta.Typed(Primitive.class)
        Collection<Primitive> primitives();
    }

    interface Feature<T extends Type> extends TypedElement<T> {
        public enum Visibility {
    		Public,Protected,Private;
		}

        @Meta.Attribute
        boolean multiple();
    }

    interface Property extends Feature<Type> {
    }

    interface Relationship extends Feature<Entity> {
        @Meta.Reference
        Relationship opposite();
    }

    interface Operation extends Named {
        enum OperationKind {
            Action, Constructor, Event, Finder, Retriever
        }

        @Meta.Contained
        @Meta.Typed(Parameter.class)
        Collection<Parameter> parameters();

        @Meta.Attribute
        public Operation.OperationKind kind();

        @Meta.Attribute
        public Feature.Visibility visibility();
    }

    interface Parameter extends TypedElement<Type> {

    }

    interface TypedElement<T extends Type> extends Named {
        @Meta.Reference
        @Meta.Required
        T type();
    }

    interface Entity extends Type {
        @Meta.Contained
        @Meta.Typed(Property.class)
        Collection<Property> properties();

        @Meta.Reference(opposite = "subTypes")
        @Meta.Typed(Entity.class)
        Collection<Entity> superTypes();

        @Meta.Reference
        @Meta.Typed(Entity.class)
        Collection<Entity> subTypes();

        @Meta.Contained
        @Meta.Typed(Operation.class)
        Collection<Operation> operations();

        @Meta.Contained
        @Meta.Typed(Relationship.class)
        Collection<Relationship> relationships();

        @Meta.Reference(opposite = "entities")
        Namespace namespace();
        
        @Meta.Attribute
        boolean abstract_();
    }

    interface Type extends Named {

    }

    interface BasicType extends Type {

    }

    interface TupleType extends Type {
        @Meta.Contained
        @Meta.Typed(BasicType.class)
        List<BasicType> componentTypes();
    }

}
