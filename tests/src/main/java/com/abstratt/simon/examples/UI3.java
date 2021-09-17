package com.abstratt.simon.examples;

import java.util.List;

import com.abstratt.simon.examples.UI.Application;
import com.abstratt.simon.metamodel.dsl.Meta;

@Meta.Package(builtIns = {})
public interface UI3 {
	interface IPrototype {
		@Meta.Reference
		@Meta.Typed(Application.class)
		List<Application> applications();
	}
}
