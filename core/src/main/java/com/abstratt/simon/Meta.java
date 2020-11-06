package com.abstratt.simon;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public @interface Meta {
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Inherited
	public @interface Package {
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
	public @interface Attribute {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)	
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

	@Retention(RetentionPolicy.RUNTIME)
	//TODO what are these for again?
	public @interface ObjectType {
	}

	@Retention(RetentionPolicy.RUNTIME)
	//TODO what are these for again?	
	public @interface RecordType {
	}
}
