package com.abstratt.simon.compiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.junit.jupiter.api.Test;

import com.abstratt.simon.compiler.Metamodel.Type;
import com.abstratt.simon.compiler.ecore.EcoreMetamodel;
import com.abstratt.simon.compiler.ecore.EcoreModelBuilder;
import com.abstratt.simon.examples.UI;
import com.abstratt.simon.java2ecore.Java2EcoreMapper;

public class CompilerTests {
	private static EPackage UI_PACKAGE = new Java2EcoreMapper().map(UI.class);
	private static EClass applicationClass = (EClass) UI_PACKAGE.getEClassifier("Application");
	private static EClass buttonClass = (EClass) UI_PACKAGE.getEClassifier("Button");
	private static EClass namedClass = (EClass) UI_PACKAGE.getEClassifier("Named");

	@Test
	void metamodelResolveType() {
		Metamodel metamodel = new EcoreMetamodel(UI_PACKAGE);
		Type resolved = metamodel.resolveType("Application");
		assertNotNull(resolved);
	}

	@Test
	void emptyApplication() {
		emptyApplication("application MyApplication {}");
	}

	@Test
	void emptyApplicationWithNameAsProperty() {
		emptyApplication("application (name = 'MyApplication')");
	}
	
	@Test
	void numericalSlot() {
		EObject rootObject = compile("button (index = 3)");
		EStructuralFeature indexFeature = buttonClass.getEStructuralFeature("index");
		int buttonIndex = (int) rootObject.eGet(indexFeature);
		assertEquals(3, buttonIndex);
	}

	private void emptyApplication(String toParse) {
		EObject rootObject = compile(toParse);
		EStructuralFeature nameFeature = applicationClass.getEStructuralFeature("name");
		String applicationName = (String) rootObject.eGet(nameFeature);
		assertEquals("MyApplication", applicationName);
	}

	private EObject compile(String toParse) {
		Metamodel metamodel = new EcoreMetamodel(UI_PACKAGE);
		EcoreModelBuilder modelBuilder = new EcoreModelBuilder();
		SimonCompiler<EObject> compiler = new SimonCompiler<>(metamodel, modelBuilder);
		EObject rootObject = compiler.compile(toParse);
		assertNotNull(rootObject);
		return rootObject;
	}

	@Test
	void applicationWithScreens() {
		EObject myApplication = compile("application MyApplication { screens { screen Screen1 {} screen Screen2 {} screen Screen3 {} } }");
		assertNotNull(myApplication);
		assertSame(applicationClass, myApplication.eClass());
		EReference screensFeature = (EReference) applicationClass.getEStructuralFeature("screens");
		@SuppressWarnings("unchecked")
		List<EObject> screens = (List<EObject>) myApplication.eGet(screensFeature);
		assertEquals(3, screens.size());
		EStructuralFeature nameFeature = namedClass.getEStructuralFeature("name");
		for (int i = 0; i < screens.size(); i++) {
			assertEquals("Screen" + (i + 1), screens.get(i).eGet(nameFeature));	
		}
	}

}
