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
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.ANNOTATION_TYPE)
    @Inherited
    @interface Type {
        enum Nature {
            Primitive, Object, Record, Enumeration
        }

        Nature value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    @interface Package {
        String[] builtIns();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    @interface Composite {
        boolean root() default false;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Contained {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Parent {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Reference {
        String opposite() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Inherited
    @interface Attribute {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Inherited
    @interface Typed {
        Class<?> value();
        boolean multivalued() default false;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    @interface Named {
        String name();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Name {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Required {
        boolean value() default true;
    }

    /**
     * ObjectTypes are the default classification, so usually do not need to be
     * explicitly used.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @Type(Type.Nature.Object)
    @interface ObjectType {}

    /**
     * A record is an structured data type that can only hold primitives,
     * enumeration values and other records.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @Type(Type.Nature.Record)
    @interface RecordType {}

    /**
     * For enums only? No, for classes as well. (i.e. either enum values or regular
     * classes will be interpreted as primitives)
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Type(Type.Nature.Primitive)
    @interface PrimitiveType {
        PrimitiveKind value();
    }
}
