package com.abstratt.simon.compiler.ecore;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.emf.ecore.EClassifier;
import org.junit.jupiter.api.Test;

import com.abstratt.simon.compiler.ecore.EcoreMetamodel.EcoreType;

public class TypeSourceTests {
	@Test
	void dynamic() {
		EcoreDynamicTypeSource typeSource = new EcoreDynamicTypeSource();
		EcoreType<EClassifier> resolved = typeSource.resolveType("application");
		assertNotNull(resolved);
	}
}
