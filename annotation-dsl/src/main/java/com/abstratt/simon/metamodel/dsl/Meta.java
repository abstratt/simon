package com.abstratt.simon.metamodel.dsl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.abstratt.simon.metamodel.Metamodel;
import com.abstratt.simon.metamodel.dsl.Meta.Type.Nature;

/**
 * An annotation-based internal DSL for creating Java-based Simon metamodels.
 */
public @interface Meta {
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.ANNOTATION_TYPE)
	@Inherited
	public @interface Type {
		enum Nature {
			Primitive, Object, Record, Enumeration
		}

		Nature value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Inherited
	public @interface Package {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Inherited
	public @interface Composite {
		boolean root() default false;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface Contained {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface Parent {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface Reference {
		String opposite() default "";
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@Inherited
	public @interface Attribute {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@Inherited
	public @interface Typed {
		Class<?> value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Inherited
	public @interface Named {
		String name();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface Name {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface Required {
		boolean value() default true;
	}

	/**
	 * ObjectTypes are the default classification, so usually do not need to be
	 * explicitly used.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@Type(Nature.Object)
	public @interface ObjectType {
	}

	/**
	 * A record is an structured data type that can only hold primitives,
	 * enumeration values and other records.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@Type(Nature.Record)
	public @interface RecordType {
	}

	/**
	 * For enums only? No, for classes as well. (i.e. either enum values or regular
	 * classes will be interpreted as primitives)
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Type(Nature.Primitive)
	public @interface PrimitiveType {
		Metamodel.PrimitiveKind value();
	}
}
