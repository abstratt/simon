package com.abstratt.simon.compiler;

import static com.abstratt.simon.compiler.TestHelper.UI_PACKAGE;
import static com.abstratt.simon.compiler.TestHelper.compileResource;
import static com.abstratt.simon.compiler.TestHelper.eClassFor;
import static com.abstratt.simon.compiler.ecore.EcoreHelper.findByFeature;
import static com.abstratt.simon.compiler.ecore.EcoreHelper.getValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.Test;

import com.abstratt.simon.compiler.Configuration.Provider;
import com.abstratt.simon.compiler.Metamodel.Type;
import com.abstratt.simon.compiler.SimonCompiler.Result;
import com.abstratt.simon.compiler.ecore.EPackageTypeSource;
import com.abstratt.simon.compiler.ecore.EcoreMetamodel.EcoreObjectType;
import com.abstratt.simon.compiler.ecore.EcoreMetamodel.EcoreSlotted;
import com.abstratt.simon.compiler.ecore.EcoreModelBuilder;
import com.abstratt.simon.examples.UI;
public class CompilerTests {
	private static EClass applicationClass = eClassFor(UI.Application.class);
	private static EClass buttonClass = eClassFor(UI.Button.class);
	private static EClass namedClass = eClassFor(UI.Named.class);
	private static EClass containerClass = eClassFor(UI.Container.class);
	private static EClass screenClass = eClassFor(UI.Screen.class);

	@Test
	void metamodelResolveType() {
		TypeSource<?> metamodel = new EPackageTypeSource(UI_PACKAGE);
		Type resolved = metamodel.resolveType("Application");
		assertNotNull(resolved);
	}

	@Test
	void emptyApplication() {
		emptyApplication("Application myApplication {}");
	}

	@Test
	void emptyApplicationWithNameAsProperty() {
		emptyApplication("Application (name = 'myApplication')");
	}
	
	@Test
	void numericalSlot() {
		EObject button = compile("Button (index = 3)");
		int buttonIndex = (int) getValue(button, "index");
		assertEquals(3, buttonIndex);
	}
	
	@Test
	void recordSlot() {
		EObject rootObject = compile("Button (backgroundColor = #(red = 100 blue = 50))");
		EObject backgroundColor = (EObject) getValue(rootObject, "backgroundColor");
		assertNotNull(backgroundColor);
		assertEquals(100, (int) getValue(backgroundColor, "red"));
		assertEquals(50, (int) getValue(backgroundColor, "blue"));
		assertEquals(0, (int) getValue(backgroundColor, "green"));

	}

	private void emptyApplication(String toParse) {
		EObject application = compile(toParse);
		assertEquals("myApplication", getValue(application, "name"));
	}

	private static EObject compile(String toParse) {
		EPackageTypeSource typeSource = new EPackageTypeSource(UI_PACKAGE);
		Provider<EcoreObjectType, EcoreSlotted<?>, EObject> modelBuilder = new EcoreModelBuilder();
		SimonCompiler<EObject> compiler = new SimonCompiler<EObject>(typeSource, modelBuilder);
		Result<EObject> result = compiler.compile(toParse);
		ensureSuccess(result);
		EObject rootObject = result.getRootObject();
		assertNotNull(rootObject);
		return rootObject;
	}

	private static  void ensureSuccess(Result<EObject> result) {
		assertEquals(0, result.getProblems().size(), result.getProblems()::toString);
	}
	
	@Test
	void applicationWithScreens() {
		EObject myApplication = compile("Application myApplication { screens { Screen screen1 {} Screen screen2 {} Screen screen3 {} } }");
		assertNotNull(myApplication);
		assertSame(applicationClass, myApplication.eClass());
		@SuppressWarnings("unchecked")
		List<EObject> screens = (List<EObject>) getValue(myApplication, "screens");
		assertEquals(3, screens.size());
		for (int i = 0; i < screens.size(); i++) {
			assertEquals("screen" + (i + 1), getValue(screens.get(i), "name"));	
		}
	}

	@Test
	void kirraProgram() {
		EObject namespace = compileResource("/kirra-sample.simon");
		List<EObject> entities = (List<EObject>) getValue(namespace, "entities");
		assertEquals(5, entities.size());
		EObject memberEntity = findByFeature(entities, "name", "Member");
		EObject memberEntityNameProperty = findByFeature(getValue(memberEntity, "properties"), "name", "name");
		assertEquals("String", getValue(memberEntityNameProperty, "type"));
	}
	
	@Test
	void uiProgram() {
		EObject application = compileResource("/ui-sample.simon");
		List<EObject> screens = (List<EObject>) getValue(application, "screens");
		assertEquals(3, screens.size());
		EObject firstScreen = screens.get(0);
		EEnumLiteral layout = (EEnumLiteral) getValue(firstScreen, "layout");
		assertEquals(UI.PanelLayout.Vertical.name(), layout.getLiteral());
		List<EObject> screenComponents = (List<EObject>) getValue(firstScreen, "children");
		assertEquals(3, screenComponents.size());
		EObject firstButton = screenComponents.get(0);
		assertEquals("Ok", getValue(firstButton, "label"));
		EObject secondButton = screenComponents.get(1);
		assertEquals("Cancel", getValue(secondButton, "label"));
		EObject backgroundColor = (EObject) getValue(secondButton, "backgroundColor");
		assertNotNull(backgroundColor);
		assertEquals(100, (int) getValue(backgroundColor, "red"));
		
		EObject link = screenComponents.get(2);
		assertEquals(screens.get(1), getValue(link, "targetScreen"));
	}
}
