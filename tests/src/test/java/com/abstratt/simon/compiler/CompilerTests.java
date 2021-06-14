package com.abstratt.simon.compiler;

import static com.abstratt.simon.compiler.ecore.EcoreHelper.findByFeature;
import static com.abstratt.simon.compiler.ecore.EcoreHelper.getValue;
import static com.abstratt.simon.testing.TestHelper.UI_PACKAGE;
import static com.abstratt.simon.testing.TestHelper.KIRRA_PACKAGE;
import static com.abstratt.simon.testing.TestHelper.compileResourceToEObject;
import static com.abstratt.simon.testing.TestHelper.uiClassFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.junit.jupiter.api.Test;

import com.abstratt.simon.compiler.Configuration.Provider;
import com.abstratt.simon.compiler.SimonCompiler.Result;
import com.abstratt.simon.compiler.ecore.EPackageTypeSource;
import com.abstratt.simon.compiler.ecore.EcoreHelper;
import com.abstratt.simon.compiler.ecore.EcoreMetamodel.EcoreObjectType;
import com.abstratt.simon.compiler.ecore.EcoreMetamodel.EcoreSlotted;
import com.abstratt.simon.compiler.ecore.EcoreModelBuilder;
import com.abstratt.simon.examples.UI;
import com.abstratt.simon.metamodel.Metamodel.Type;
import com.abstratt.simon.testing.TestHelper;

public class CompilerTests {
	private static EClass applicationClass = uiClassFor(UI.Application.class);
	private static EClass buttonClass = uiClassFor(UI.Button.class);
	private static EClass namedClass = uiClassFor(UI.Named.class);
	private static EClass containerClass = uiClassFor(UI.Container.class);
	private static EClass screenClass = uiClassFor(UI.Screen.class);

	@Test
	void metamodelResolveType() {
		TypeSource<?> metamodel = new EPackageTypeSource(UI_PACKAGE);
		Type resolved = metamodel.resolveType("Application");
		assertNotNull(resolved);
	}

	@Test
	void metamodelResolvePrimitive() {
		EPackageTypeSource metamodel = new EPackageTypeSource(KIRRA_PACKAGE);
		EPackage package_ = metamodel.getPackage();
		Type resolved = metamodel.resolveType("StringValue");
		EcoreHelper.tree(package_).forEach(System.out::println);
		assertNotNull(resolved);
	}

	@Test
	void emptyApplication() {
		EObject application = emptyApplication("Application myApplication {}");
		assertNotNull(application.eResource());
	}
	
	@Test
	void emptyApplications() {
		List<Result<EObject>> results = compileProject(UI_PACKAGE, "Application myApplication1 {}", "Application myApplication2 {}");
		EObject application1 = results.get(0).getRootObject();
		EObject application2 = results.get(1).getRootObject();
		assertEquals("myApplication1", TestHelper.getPrimitiveValue(application1, "name"));
		assertEquals("myApplication2", TestHelper.getPrimitiveValue(application2, "name"));
		assertNotNull(application1.eResource());
		assertSame(application1.eResource(), application2.eResource());
	}
	
	@Test
	void importApplication() {
		emptyApplication("Application myApplication {}");
	}

	@Test
	void emptyApplication_metaclassCapitalization() {
		emptyApplication("application myApplication {}");
	}

	@Test
	void emptyApplicationWithNameAsProperty() {
		emptyApplication("Application (name = 'myApplication')");
	}

	@Test
	void numericalSlot() {
		EObject button = compileUI("Button (index = 3)");
		int buttonIndex = (int) TestHelper.getPrimitiveValue(button, "index");
		assertEquals(3, buttonIndex);
	}

	@Test // issue https://github.com/abstratt/simon/issues/3
	void primitiveTypes() {
		String toParse = "Namespace { entities { Entity Product { properties { Property description { type = StringValue } } } } }";
		EObject namespace = compileKirra(toParse);
		List<EObject> entities = getValue(namespace, "entities");
		assertEquals(1, entities.size());
		EObject productEntity = entities.get(0);
		EObject descriptionProperty = findByFeature(getValue(productEntity, "properties"), "name", "description");
		assertEquals("StringValue", TestHelper.getPrimitiveValue(descriptionProperty, "type"));
	}

	@Test
	void recordSlot() {
		EObject rootObject = compileUI("Button (backgroundColor = #(red = 100 blue = 50))");
		EObject backgroundColor = (EObject) getValue(rootObject, "backgroundColor");
		assertNotNull(backgroundColor);
		assertEquals(100, (int) TestHelper.getPrimitiveValue(backgroundColor, "red"));
		assertEquals(50, (int) TestHelper.getPrimitiveValue(backgroundColor, "blue"));
		assertEquals(0, (int) TestHelper.getPrimitiveValue(backgroundColor, "green"));

	}

	private EObject emptyApplication(String toParse) {
		EObject application = compileUI(toParse);
		assertEquals("myApplication", TestHelper.getPrimitiveValue(application, "name"));
		return application;
	}

	private static EObject compileUI(String toParse) {
		return compile(UI_PACKAGE, toParse);
	}
	
	private static EObject compileKirra(String toParse) {
		return compile(KIRRA_PACKAGE, toParse);
	}

	private static EObject compile(EPackage package_, String toParse) {
		List<Result<EObject>> models = compileProject(package_, toParse);
		assertEquals(1, models.size());
		assertNotNull(models.get(0));
		return models.get(0).getRootObject();
	}

	private static List<Result<EObject>> compileProject(EPackage package_, String... toParse) {
		EPackageTypeSource typeSource = new EPackageTypeSource(package_);
		Provider<EcoreObjectType, EcoreSlotted<?>, EObject> modelBuilder = new EcoreModelBuilder();
		SimonCompiler<EObject> compiler = new SimonCompiler<EObject>(typeSource, modelBuilder);
		List<Result<EObject>> results = compiler.compile(toParse);
		results.forEach(CompilerTests::ensureSuccess);
		return results;
	}

	private static void ensureSuccess(Result<?> result) {
		assertEquals(0, result.getProblems().size(), result.getProblems()::toString);
	}

	@Test
	void applicationWithScreens() {
		EObject myApplication = compileUI(
				"Application myApplication { screens { Screen screen1 {} Screen screen2 {} Screen screen3 {} } }");
		assertNotNull(myApplication);
		assertSame(applicationClass, myApplication.eClass());
		@SuppressWarnings("unchecked")
		List<EObject> screens = (List<EObject>) getValue(myApplication, "screens");
		assertEquals(3, screens.size());
		for (int i = 0; i < screens.size(); i++) {
			assertEquals("screen" + (i + 1), TestHelper.getPrimitiveValue(screens.get(i), "name"));
		}
	}

	@Test
	void kirraProgram() {
		EObject namespace = compileResourceToEObject("/kirra-sample.simon");
		List<EObject> entities = (List<EObject>) getValue(namespace, "entities");
		assertEquals(5, entities.size());
		EObject memberEntity = findByFeature(entities, "name", "Member");
		EObject memberEntityNameProperty = findByFeature(getValue(memberEntity, "properties"), "name", "name");
		assertEquals("StringValue", TestHelper.getPrimitiveValue(memberEntityNameProperty, "type"));
	}

	@Test
	void uiProgram() {
		EObject application = compileResourceToEObject("/ui-sample.simon");
		List<EObject> screens = (List<EObject>) getValue(application, "screens");
		assertEquals(3, screens.size());
		EObject firstScreen = screens.get(0);
		EEnumLiteral layout = (EEnumLiteral) getValue(firstScreen, "layout");
		assertEquals(UI.PanelLayout.Vertical.name(), layout.getLiteral());
		List<EObject> screenComponents = (List<EObject>) getValue(firstScreen, "children");
		assertEquals(3, screenComponents.size());
		EObject firstButton = screenComponents.get(0);
		assertEquals("Ok", TestHelper.getPrimitiveValue(firstButton, "label"));
		EObject secondButton = screenComponents.get(1);
		assertEquals("Cancel", TestHelper.getPrimitiveValue(secondButton, "label"));
		EObject backgroundColor = (EObject) getValue(secondButton, "backgroundColor");
		assertNotNull(backgroundColor);
		assertEquals(100, (int) TestHelper.getPrimitiveValue(backgroundColor, "red"));

		EObject link = screenComponents.get(2);
		assertEquals(screens.get(1), getValue(link, "targetScreen"));

	}
}
