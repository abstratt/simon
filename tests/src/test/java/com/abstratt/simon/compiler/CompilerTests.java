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
import com.abstratt.simon.compiler.ecore.EPackageTypeSource;
import com.abstratt.simon.compiler.ecore.EcoreDynamicTypeSource;
import com.abstratt.simon.compiler.ecore.EcoreMetamodel;
import com.abstratt.simon.compiler.ecore.EcoreMetamodel.EcoreObjectType;
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
		EObject rootObject = compile("button (index = 3)");
		EStructuralFeature indexFeature = findStructuralFeature(buttonClass, "index");
		int buttonIndex = (int) rootObject.eGet(indexFeature);
		assertEquals(3, buttonIndex);
	}

	private EStructuralFeature findStructuralFeature(EClass eClass, String featureName) {
		return eClass.getEAllStructuralFeatures().stream().filter(feature -> feature.getName().equals(featureName)).findAny().orElse(null);
	}
	
	private EStructuralFeature findStructuralFeature(EObject eObject, String featureName) {
		return findStructuralFeature(eObject.eClass(), featureName);
	}


	private void emptyApplication(String toParse) {
		EObject rootObject = compile(toParse);
		EStructuralFeature nameFeature = findStructuralFeature(applicationClass, "name");
		String applicationName = (String) rootObject.eGet(nameFeature);
		assertEquals("MyApplication", applicationName);
	}

	private EObject compile(String toParse) {
		EPackageTypeSource typeSource = new EPackageTypeSource(UI_PACKAGE);
		Metamodel metamodel = new EcoreMetamodel();
		Provider<EcoreObjectType, EObject> modelBuilder = new EcoreModelBuilder();
		SimonCompiler<EObject> compiler = new SimonCompiler<EObject>(metamodel, typeSource, modelBuilder);
		EObject rootObject = compiler.compile(toParse);
		assertNotNull(rootObject);
		return rootObject;
	}
	
	private EObject compileResource(String path) {
		URL resourceUrl = CompilerTests.class.getResource(path);
		assertNotNull(resourceUrl, () -> "Resource not found: " + path);
		Metamodel metamodel = new EcoreMetamodel();
		Provider<EcoreObjectType, EObject> modelBuilder = new EcoreModelBuilder();
		TypeSource typeSource = new EcoreDynamicTypeSource();
		SimonCompiler<EObject> compiler = new SimonCompiler<EObject>(metamodel, typeSource , modelBuilder);
		EObject rootObject = compiler.compile(resourceUrl);
		assertNotNull(rootObject);
		return rootObject;
	}


	@Test
	void applicationWithScreens() {
		EObject myApplication = compile("application MyApplication { screens { screen Screen1 {} screen Screen2 {} screen Screen3 {} } }");
		assertNotNull(myApplication);
		assertSame(applicationClass, myApplication.eClass());
		EReference screensFeature = (EReference) findStructuralFeature(applicationClass, "screens");
		@SuppressWarnings("unchecked")
		List<EObject> screens = (List<EObject>) myApplication.eGet(screensFeature);
		assertEquals(3, screens.size());
		EStructuralFeature nameFeature = findStructuralFeature(namedClass, "name");
		for (int i = 0; i < screens.size(); i++) {
			assertEquals("Screen" + (i + 1), screens.get(i).eGet(nameFeature));	
		}
	}
	
	@Test
	void program() {
		EObject application = compileResource("/ui-sample.simon");
		List<EObject> screens = (List<EObject>) application.eGet(findStructuralFeature(application, "screens"));
		assertEquals(3, screens.size());
		EObject firstScreen = screens.get(0);
		EEnumLiteral layout = (EEnumLiteral) firstScreen.eGet(findStructuralFeature(firstScreen, "layout"));
		assertEquals(UI.PanelLayout.Vertical.name(), layout.getLiteral());
		List<EObject> screenComponents = (List<EObject>) firstScreen.eGet(findStructuralFeature(firstScreen, "children"));
		assertEquals(2, screenComponents.size());
		EObject firstButton = screenComponents.get(0);
		assertEquals("Ok", firstButton.eGet(findStructuralFeature(firstButton, "label")));
	}


}
