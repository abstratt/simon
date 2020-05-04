package com.abstratt.simon;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public @interface Meta {
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Package {
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface Contained {
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface Parent {
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface Reference {
		String opposite() default "";
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface Attribute {
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface Typed {
		Class<?> value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface Named {
		String name();
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface Name {
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface Required {
		boolean value() default true;
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface ObjectType {
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface RecordType {
	}
	
}
