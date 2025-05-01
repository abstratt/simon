package com.abstratt.simon.metamodel.dsl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.abstratt.simon.metamodel.Metamodel.PrimitiveKind;

/**
 * An annotation-based internal DSL for creating Java-based Simon metamodels.
 */
public @interface Meta {
    /**
     * Defines the nature of the type.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.ANNOTATION_TYPE)
    @Inherited
    @interface Type {
        /**
         * Specifies the nature of the type.
         */
        enum Nature {
            Primitive, Object, Record, Enumeration
        }

        /**
         * Specifies the nature of the type.
         */
        Nature value();
    }

    /**
     * Specifies the built-in types for the package.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    @interface Package {
        /**
         * Specifies the built-in types for the package.
         */
        String[] builtIns();
    }

    /**
     * Indicates whether the composite is a root element.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    @interface Composite {
        /**
         * Indicates whether the composite is a root element.
         */
        boolean root() default false;
    }

    /**
     * Marks a method as a contained element.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Contained {
        // No additional attributes
    }

    /**
     * Marks a method as a parent element.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Parent {
        // No additional attributes
    }

    /**
     * Marks a method as a reference element.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Reference {
        /**
         * Specifies the opposite reference name.
         */
        String opposite() default "";
    }

    /**
     * Marks a method as an attribute element.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Inherited
    @interface Attribute {
        // No additional attributes
    }

    /**
     * Specifies the type of the element.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Inherited
    @interface Typed {
        /**
         * Specifies the type of the element.
         */
        Class<?> value();

        /**
         * Indicates whether the element is multivalued.
         */
        boolean multivalued() default false;
    }

    /**
     * Specifies the name of the element.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    @interface Named {
        /**
         * Specifies the name of the element.
         */
        String name();
    }

    /**
     * Marks a method as the name of the element.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Name {
        // No additional attributes
    }

    /**
     * Indicates whether the element is required.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Required {
        /**
         * Indicates whether the element is required.
         */
        boolean value() default true;
    }

    /**
     * ObjectTypes are the default classification, so usually do not need to be
     * explicitly used.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @Type(Type.Nature.Object)
    @interface ObjectType {
        /**
         * Indicates whether the object type is instantiable.
         */
        boolean instantiable() default true;
    }

    /**
     * A record is a structured data type that can only hold primitives,
     * enumeration values and other records.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @Type(Type.Nature.Record)
    @interface RecordType {
        // No additional attributes
    }

    /**
     * For enums only? No, for classes as well. (i.e. either enum values or regular
     * classes will be interpreted as primitives)
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Type(Type.Nature.Primitive)
    @interface PrimitiveType {
        /**
         * Specifies the kind of primitive type.
         */
        PrimitiveKind value();
    }
}
