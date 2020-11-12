package com.abstratt.simon.compiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URL;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

import com.abstratt.simon.compiler.Configuration.Provider;
import com.abstratt.simon.compiler.SimonCompiler.Result;
import com.abstratt.simon.compiler.ecore.EPackageTypeSource;
import com.abstratt.simon.compiler.ecore.EcoreDynamicTypeSource;
import com.abstratt.simon.compiler.ecore.EcoreModelBuilder;
import com.abstratt.simon.compiler.ecore.EcoreMetamodel.EcoreObjectType;
import com.abstratt.simon.compiler.ecore.EcoreMetamodel.EcoreSlotted;
import com.abstratt.simon.examples.UI;
import com.abstratt.simon.java2ecore.Java2EcoreMapper;

public class TestHelper {
	
	static EPackage UI_PACKAGE = new Java2EcoreMapper().map(UI.class);
	public static EClass eClassFor(Class<?> clazz) {
		return (EClass) UI_PACKAGE.getEClassifier(clazz.getSimpleName());
	}

	
	public static EObject compile(String toParse) {
		EPackageTypeSource typeSource = new EPackageTypeSource(UI_PACKAGE);
		Provider<EcoreObjectType, EcoreSlotted<?>, EObject> modelBuilder = new EcoreModelBuilder();
		SimonCompiler<EObject> compiler = new SimonCompiler<EObject>(typeSource, modelBuilder);
		Result<EObject> result = compiler.compile(toParse);
		ensureSuccess(result);
		EObject rootObject = result.getRootObject();
		assertNotNull(rootObject);
		return rootObject;
	}
	
	public static EObject compileResource(String path) {
		URL resourceUrl = CompilerTests.class.getResource(path);
		assertNotNull(resourceUrl, () -> "Resource not found: " + path);
		Provider<EcoreObjectType, EcoreSlotted<?>, EObject> modelBuilder = new EcoreModelBuilder();
		TypeSource<?> typeSource = new EcoreDynamicTypeSource();
		SimonCompiler<EObject> compiler = new SimonCompiler<EObject>(typeSource, modelBuilder);
		Result<EObject> result = compiler.compile(resourceUrl);
		ensureSuccess(result);
		EObject rootObject = result.getRootObject();
		assertNotNull(rootObject);
		return rootObject;
	}

	private static  void ensureSuccess(Result<EObject> result) {
		assertEquals(0, result.getProblems().size(), result.getProblems()::toString);
	}
}
