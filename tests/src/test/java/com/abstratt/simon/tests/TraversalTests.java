package com.abstratt.simon.tests;

import static com.abstratt.simon.testing.TestHelper.compileResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.abstratt.simon.compiler.Result;
import com.abstratt.simon.compiler.backend.ecore.EObjectTraversalProvider;
import com.abstratt.simon.examples.ui.UI;
import com.abstratt.simon.genutils.Traversal;
import com.abstratt.simon.metamodel.ecore.impl.EcoreHelper;
import com.abstratt.simon.testing.TestHelper;

public class TraversalTests {
	private static EObject application;
	private static EObject screen1;
	private static EObject screen2;
	private static EObject button1a;
	private static EObject button1b;
	private static EObject link;
	private static EObject button2a;
	private static EAttribute nameAttribute;
	private static List<Result<EObject>> setupResults;

	public static <O> O getValue(EObject eObject, String featureName) {
		return Objects.requireNonNull(EcoreHelper.getValue(eObject, featureName));
	}

	@BeforeAll
	static void setup() throws Exception {
		List<Result<EObject>> results = compileResource(UI.class, "/ui-sample.simon");
		setupResults = results;
		application = results.get(0).getRootObject();
		screen1 = ((EList<EObject>) getValue(application, "screens")).get(0);
		screen2 = ((EList<EObject>) getValue(application, "screens")).get(1);
		button1a = ((EList<EObject>) getValue(screen1, "children")).get(0);
		button1b = ((EList<EObject>) getValue(screen1, "children")).get(1);
		button2a = ((EList<EObject>) getValue(screen2, "children")).get(0);
		link = ((EList<EObject>) getValue(screen1, "children")).get(2);
		nameAttribute = EcoreHelper.findInList(application.eClass().getEAllAttributes(),
				attr -> "name".equals(attr.getName()));
	}

	@Test
	void setupChecks() {
		TestHelper.ensureSuccess(setupResults);
	}

	@Test
	void bubbleUp() {
		assertSame(screen2, provider().bubbleUp(provider().indexedFeature("screens", 1)).hop(button1a));
		assertSame(button1a, provider().bubbleUp(provider().self()).hop(button1a));
	}

	@Test
	void bubbleUpSimple() {
		Traversal traversal = provider().bubbleUp(provider().named("myApplication"));
		assertSame(application, traversal.hop(screen1));
		assertSame(application, traversal.hop(button1a));
	}

	@Test
	void condition() {
		assertSame(screen1, provider().condition(it -> true).hop(screen1));
		assertNull(provider().condition(it -> false).hop(screen1));
	}

	@Test
	void named() {
		assertSame(screen1, provider().named("screen1").hop(screen1));
		assertNull(provider().named("screen2").hop(screen1));
		assertSame(application, provider().named("myApplication").hop(application));
	}
	
	@Test
	void container() {
		assertSame(application, provider().container().hop(screen1));
	}

	@Test
	void grandContainer() {
		assertSame(application, provider().compose(provider().container(), provider().container()).hop(button1a));
	}

	@Test
	void any() {
		assertSame(screen1, provider().any(provider().list("foobar", e -> false), provider().list("screens", e -> true))
				.hop(application));
	}

	@Test
	void then() {
		assertSame(button2a, provider().self().then(provider().to(button2a)).hop(button1a));
		assertSame(button1a, provider().self().then(provider().self()).hop(button1a));
		assertSame(button1a, provider().self().then(provider().indexedFeature("children", 0)).hop(screen1));
		assertSame(screen1, provider().to(button1a).then(provider().feature("parent")).hop(application));
	}

	@Test
	void compose() {
		assertSame(button1a, provider().compose(provider().to(button1a)).hop(application));
		assertSame(screen1, provider().compose(provider().to(button1a), provider().feature("parent")).hop(application));
		assertSame(screen1, provider().compose(provider().to(button1a), provider().feature("parent"),
				provider().indexedFeature("children", 1), provider().feature("parent")).hop(application));
		assertSame(button1b, provider()
				.compose(provider().to(button1a), provider().feature("parent"), provider().indexedFeature("children", 1))
				.hop(application));
	}

	@Test
	void simple() {
		assertSame(button1a, provider().self().hop(button1a));
		assertSame(button2a, provider().to(button2a).hop(button1a));
	}

	@Test
	void childWithAttributeValued() {
		assertSame(screen1, provider().childWithAttributeValued(nameAttribute, "screen1").hop(application));
		assertSame(button1a, provider().childWithAttributeValued(nameAttribute, "btn1a").hop(screen1));
	}

	@Test
	void attributeValued() {
		assertSame(application, provider().attributeValued(nameAttribute, "myApplication").hop(application));
		assertNull(provider().attributeValued(nameAttribute, "myApplication2").hop(application));
	}

	@Test
	void indexedFeature() {
		assertSame(screen1, provider().indexedFeature("screens", 0).hop(application));
		assertSame(button1b, provider().indexedFeature("children", 1).hop(screen1));
	}

	@Test
	void feature() {
		assertSame(screen1, provider().feature("parent").hop(button1a));
		assertSame(application, provider().feature("application").hop(screen1));
	}

	@Test
	void listFeature() {
		assertSame(screen2,
				provider().list("screens", e -> "screen2".equals(TestHelper.getPrimitiveValue(e, "name"))).hop(application));
		assertNull(provider().list("screens", e -> "screen999".equals(getValue(e, "name"))).hop(application));
	}

	@Test
	void hierarchy() {
		assertEquals(Arrays.asList(application), provider().hierarchy(application).collect(Collectors.toList()));
		assertEquals(Arrays.asList(screen1, application), provider().hierarchy(screen1).collect(Collectors.toList()));
		assertEquals(Arrays.asList(button1a, screen1, application),
				provider().hierarchy(button1a).collect(Collectors.toList()));
		assertEquals(Arrays.asList(button2a, screen2, application),
				provider().hierarchy(button2a).collect(Collectors.toList()));
	}

	@Test
	void children() {
		assertEquals(Collections.emptyList(), provider().children().enumerate(button1a).collect(Collectors.toList()));
	}

	@Test
	void roots() {
		assertEquals(Collections.singletonList(application), provider().roots().enumerate(button1a).collect(Collectors.toList()));
	}

	@Test
	void searchingSanityChecks() {
		assertEquals("screen1", TestHelper.getPrimitiveValue(screen1, "name"));
		assertEquals("screen2", TestHelper.getPrimitiveValue(screen2, "name"));
		assertEquals("btn1a", TestHelper.getPrimitiveValue(button1a, "name"));
		assertEquals("btn1b", TestHelper.getPrimitiveValue(button1b, "name"));
		assertEquals("btn2a", TestHelper.getPrimitiveValue(button2a, "name"));
	}

	@Test
	void searchingUp() {
		assertSame(screen2, search(nameAttribute, "screen2").hop(screen1));
		assertSame(screen2, search(nameAttribute, "screen2").hop(button1b));
		assertSame(screen2, search(nameAttribute, "screen2").hop(screen2));
		assertSame(application, search(nameAttribute, "myApplication").hop(application));
		assertSame(application, search(nameAttribute, "myApplication").hop(screen1));
		assertSame(application, search(nameAttribute, "myApplication").hop(button1b));
	}

	@Test
	void searchingImmediate() {
		assertSame(screen2, search(nameAttribute, "screen2").hop(application));
		assertSame(screen1, search(nameAttribute, "screen1").hop(application));
		assertSame(button1b, search(nameAttribute, "btn1b").hop(screen1));
	}

	@Test
	void searchingDeep() {
		assertSame(button1b, search(nameAttribute, "screen1", "btn1b").hop(screen1));
		assertSame(button1b, search(nameAttribute, "screen1", "btn1b").hop(button1b));
		assertSame(button1b, search(nameAttribute, "screen1", "btn1b").hop(screen2));
		assertSame(button1b, search(nameAttribute, "screen1", "btn1b").hop(application));
		assertNull(search(nameAttribute, "btn2b").hop(screen1));
	}

	@Test
	void searchingGrandChildren() {
		assertSame(button1b, provider().children().then(search(nameAttribute, "btn1b")).hop(application));
		assertSame(button2a, provider().children().then(search(nameAttribute, "btn2a")).hop(application));
	}
	
	private Traversal.Provider<EObject, EAttribute> provider() {
		return EObjectTraversalProvider.INSTANCE;
	}
	private Traversal<EObject> search(EAttribute nameAttribute, Object... values) {
		return provider().search(nameAttribute, values);
	}
}
