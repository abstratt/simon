package com.abstratt.simon.compiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.net.URL;
import java.util.List;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.junit.jupiter.api.Test;

import com.abstratt.simon.compiler.Configuration.Provider;
import com.abstratt.simon.compiler.Metamodel.Type;
import com.abstratt.simon.compiler.SimonCompiler.Result;
import com.abstratt.simon.compiler.ecore.EPackageTypeSource;
import com.abstratt.simon.compiler.ecore.EcoreDynamicTypeSource;
import com.abstratt.simon.compiler.ecore.EcoreMetamodel;
import com.abstratt.simon.compiler.ecore.EcoreMetamodel.EcoreObjectType;
import com.abstratt.simon.compiler.ecore.EcoreMetamodel.EcoreSlotted;
import com.abstratt.simon.compiler.ecore.EcoreMetamodel.EcoreRecordType;
import com.abstratt.simon.compiler.ecore.EcoreModelBuilder;
import com.abstratt.simon.examples.UI;
import com.abstratt.simon.java2ecore.Java2EcoreMapper;

public class CompilerTests {
	private static EPackage UI_PACKAGE = new Java2EcoreMapper().map(UI.class);
	private static EClass eClassFor(Class<?> clazz) {
		return (EClass) UI_PACKAGE.getEClassifier(clazz.getSimpleName());
	}
	private static EClass applicationClass = eClassFor(UI.Application.class);
	private static EClass buttonClass = eClassFor(UI.Button.class);
	private static EClass namedClass = eClassFor(UI.Named.class);;
	private static EClass containerClass = eClassFor(UI.Container.class);;
	private static EClass screenClass = eClassFor(UI.Screen.class);;

	@Test
	void metamodelResolveType() {
		TypeSource<?> metamodel = new EPackageTypeSource(UI_PACKAGE);
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
		EObject button = compile("button (index = 3)");
		int buttonIndex = (int) getValue(button, "index");
		assertEquals(3, buttonIndex);
	}
	
	@Test
	void recordSlot() {
		EObject rootObject = compile("button (backgroundColor = # (red = 100 blue = 50))");
		EObject backgroundColor = (EObject) getValue(rootObject, "backgroundColor");
		assertNotNull(backgroundColor);
		assertEquals(100, (int) getValue(backgroundColor, "red"));
		assertEquals(50, (int) getValue(backgroundColor, "blue"));
		assertEquals(0, (int) getValue(backgroundColor, "green"));

	}

	private void emptyApplication(String toParse) {
		EObject application = compile(toParse);
		assertEquals("MyApplication", getValue(application, "name"));
	}

	private EObject compile(String toParse) {
		EPackageTypeSource typeSource = new EPackageTypeSource(UI_PACKAGE);
		Provider<EcoreObjectType, EcoreSlotted<?>, EObject> modelBuilder = new EcoreModelBuilder();
		SimonCompiler<EObject> compiler = new SimonCompiler<EObject>(typeSource, modelBuilder);
		Result<EObject> result = compiler.compile(toParse);
		ensureSuccess(result);
		EObject rootObject = result.getRootObject();
		assertNotNull(rootObject);
		return rootObject;
	}

	private void ensureSuccess(Result<EObject> result) {
		assertEquals(0, result.getProblems().size(), result.getProblems()::toString);
	}
	
	private EObject compileResource(String path) {
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


	@Test
	void applicationWithScreens() {
		EObject myApplication = compile("application MyApplication { screens { screen Screen1 {} screen Screen2 {} screen Screen3 {} } }");
		assertNotNull(myApplication);
		assertSame(applicationClass, myApplication.eClass());
		@SuppressWarnings("unchecked")
		List<EObject> screens = (List<EObject>) getValue(myApplication, "screens");
		assertEquals(3, screens.size());
		for (int i = 0; i < screens.size(); i++) {
			assertEquals("Screen" + (i + 1), getValue(screens.get(i), "name"));	
		}
	}
	
	@Test
	void program() {
		EObject application = compileResource("/ui-sample.simon");
		List<EObject> screens = (List<EObject>) getValue(application, "screens");
		assertEquals(3, screens.size());
		EObject firstScreen = screens.get(0);
		EEnumLiteral layout = (EEnumLiteral) getValue(firstScreen, "layout");
		assertEquals(UI.PanelLayout.Vertical.name(), layout.getLiteral());
		List<EObject> screenComponents = (List<EObject>) getValue(firstScreen, "children");
		assertEquals(2, screenComponents.size());
		EObject firstButton = screenComponents.get(0);
		assertEquals("Ok", getValue(firstButton, "label"));
		EObject secondButton = screenComponents.get(1);
		assertEquals("Cancel", getValue(secondButton, "label"));
		EObject backgroundColor = (EObject) getValue(secondButton, "backgroundColor");
		assertNotNull(backgroundColor);
		assertEquals(100, (int) getValue(backgroundColor, "red"));
	}

	private <O> O getValue(EObject eObject, String featureName) {
		return (O) eObject.eGet(findStructuralFeature(eObject, featureName));
	}


	private EStructuralFeature findStructuralFeature(EClass eClass, String featureName) {
		return eClass.getEAllStructuralFeatures().stream().filter(feature -> feature.getName().equals(featureName)).findAny().orElse(null);
	}
	
	private EStructuralFeature findStructuralFeature(EObject eObject, String featureName) {
		return findStructuralFeature(eObject.eClass(), featureName);
	}

}
