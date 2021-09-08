package com.abstratt.simon.compiler.antlr;

import com.abstratt.simon.compiler.SimonCompiler;
import com.abstratt.simon.compiler.antlr.impl.SimonCompilerAntlrImpl;
import com.abstratt.simon.compiler.backend.Backend;
import com.abstratt.simon.compiler.source.MetamodelSource.Factory;
import com.abstratt.simon.metamodel.Metamodel.ObjectType;
import com.abstratt.simon.metamodel.Metamodel.Slotted;

public class SimonCompilerAntlrFactory implements SimonCompiler.Factory {

	@Override
	public <T> SimonCompiler<T> create(Factory<?> typeSourceFactory,
			Backend<? extends ObjectType, ? extends Slotted, T> configurationProvider) {
		return new SimonCompilerAntlrImpl<>(typeSourceFactory, configurationProvider);
	}

}
