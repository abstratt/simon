package com.abstratt.simon.examples;

import java.util.Collection;

import com.abstratt.simon.metamodel.dsl.Meta;

/**
 * The bootstrap metamodel for {@code @language Simon}: lets users define
 * Simon metamodels in Simon source. Compiled instances of this metamodel
 * are translated to {@link org.eclipse.emf.ecore.EPackage}s by
 * {@code Simon2EcoreMapper}.
 */
@Meta.Package(builtIns = { "primitives" })
public interface Simon {

    enum PrimitiveKind {
        Integer, Decimal, Boolean, String, Other
    }

    enum AttributeRole {
        Plain, Name, Documentation, Modifier
    }

    @Meta.ObjectType(instantiable = false)
    interface Named {
        @Meta.Name
        @Meta.Attribute
        String name();

        @Meta.Required(false)
        @Meta.Documentation
        @Meta.Attribute
        String documentation();
    }

    @Meta.Composite(root = true)
    interface Package extends Named {
        @Meta.Contained
        @Meta.Typed(ObjectType.class)
        Collection<ObjectType> objectTypes();

        @Meta.Contained
        @Meta.Typed(RecordType.class)
        Collection<RecordType> recordTypes();

        @Meta.Contained
        @Meta.Typed(EnumType.class)
        Collection<EnumType> enumTypes();

        @Meta.Contained
        @Meta.Typed(PrimitiveType.class)
        Collection<PrimitiveType> primitiveTypes();
    }

    @Meta.ObjectType(instantiable = false)
    interface Classifier extends Named {
    }

    @Meta.Composite
    interface ObjectType extends Classifier {
        @Meta.Attribute
        @Meta.Modifier
        boolean abstract_();

        @Meta.Attribute
        @Meta.Modifier
        boolean root();

        @Meta.Reference
        @Meta.Typed(ObjectType.class)
        Collection<ObjectType> superTypes();

        @Meta.Contained
        @Meta.Typed(Attribute.class)
        Collection<Attribute> attributes();

        @Meta.Contained
        @Meta.Typed(Containment.class)
        Collection<Containment> containments();

        @Meta.Contained
        @Meta.Typed(Reference.class)
        Collection<Reference> references();
    }

    @Meta.Composite
    interface RecordType extends Classifier {
        @Meta.Contained
        @Meta.Typed(Attribute.class)
        Collection<Attribute> attributes();
    }

    @Meta.Composite
    interface EnumType extends Classifier {
        @Meta.Contained
        @Meta.Typed(EnumLiteral.class)
        Collection<EnumLiteral> literals();
    }

    interface EnumLiteral extends Named {
    }

    interface PrimitiveType extends Classifier {
        @Meta.Attribute
        @Meta.Required
        PrimitiveKind kind();
    }

    @Meta.ObjectType(instantiable = false)
    interface Feature extends Named {
        @Meta.Attribute
        @Meta.Modifier
        boolean required();

        @Meta.Attribute
        @Meta.Modifier
        boolean optional();

        @Meta.Attribute
        @Meta.Modifier
        boolean multivalued();
    }

    interface Attribute extends Feature {
        @Meta.Reference
        @Meta.Required
        Classifier type();

        @Meta.Attribute
        @Meta.Modifier
        AttributeRole role();
    }

    interface Containment extends Feature {
        @Meta.Reference
        @Meta.Required
        ObjectType type();
    }

    interface Reference extends Feature {
        @Meta.Reference
        @Meta.Required
        ObjectType type();

        @Meta.Required(false)
        @Meta.Attribute
        String opposite();
    }
}
