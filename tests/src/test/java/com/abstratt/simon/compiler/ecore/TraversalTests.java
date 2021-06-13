package com.abstratt.simon.compiler.ecore;

import static com.abstratt.simon.compiler.ecore.Traversal.search;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.abstratt.simon.compiler.SimonCompiler.Result;
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

	public static <O> O getValue(EObject eObject, String featureName) {
		return Objects.requireNonNull(EcoreHelper.getValue(eObject, featureName));
	}

	@BeforeAll
	static void setup() {
		Result<EObject> result = TestHelper.compileResource("/ui-sample.simon");
		application = result.getRootObject();
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
	void bubbleUp() {
		assertSame(screen2, Traversal.bubbleUp(Traversal.indexedFeature("screens", 1)).hop(button1a));
	}

	@Test
	void bubbleUpSimple() {
		Traversal traversal = Traversal.bubbleUp(Traversal.named("myApplication"));
		assertSame(application, traversal.hop(screen1));
		assertSame(application, traversal.hop(button1a));
	}

	@Test
	void condition() {
		assertSame(screen1, Traversal.condition(it -> true).hop(screen1));
		assertNull(Traversal.condition(it -> false).hop(screen1));
	}

	@Test
	void named() {
		assertSame(screen1, Traversal.named("screen1").hop(screen1));
		assertNull(Traversal.named("screen2").hop(screen1));
	}

	@Test
	void container() {
		assertSame(application, Traversal.container().hop(screen1));
	}

	@Test
	void grandContainer() {
		assertSame(application, Traversal.compose(Traversal.container(), Traversal.container()).hop(button1a));
	}

	@Test
	void any() {
		assertSame(screen1, Traversal.any(Traversal.list("foobar", e -> false), Traversal.list("screens", e -> true))
				.hop(application));
	}

	@Test
	void then() {
		assertSame(button2a, Traversal.self().then(Traversal.to(button2a)).hop(button1a));
		assertSame(button1a, Traversal.self().then(Traversal.self()).hop(button1a));
		assertSame(button1a, Traversal.self().then(Traversal.indexedFeature("children", 0)).hop(screen1));
		assertSame(screen1, Traversal.to(button1a).then(Traversal.feature("parent")).hop(application));
	}

	@Test
	void compose() {
		assertSame(button1a, Traversal.compose(Traversal.to(button1a)).hop(application));
		assertSame(screen1, Traversal.compose(Traversal.to(button1a), Traversal.feature("parent")).hop(application));
		assertSame(screen1, Traversal.compose(Traversal.to(button1a), Traversal.feature("parent"),
				Traversal.indexedFeature("children", 1), Traversal.feature("parent")).hop(application));
		assertSame(button1b, Traversal
				.compose(Traversal.to(button1a), Traversal.feature("parent"), Traversal.indexedFeature("children", 1))
				.hop(application));
	}

	@Test
	void simple() {
		assertSame(button1a, Traversal.self().hop(button1a));
		assertSame(button2a, Traversal.to(button2a).hop(button1a));
	}

	@Test
	void attributeValued() {
		assertSame(screen1, Traversal.childWithAttributeValued(nameAttribute, "screen1").hop(application));
		assertSame(button1a, Traversal.childWithAttributeValued(nameAttribute, "btn1a").hop(screen1));
	}

	@Test
	void indexedFeature() {
		assertSame(screen1, Traversal.indexedFeature("screens", 0).hop(application));
		assertSame(button1b, Traversal.indexedFeature("children", 1).hop(screen1));
	}

	@Test
	void feature() {
		assertSame(screen1, Traversal.feature("parent").hop(button1a));
		assertSame(application, Traversal.feature("application").hop(screen1));
	}

	@Test
	void listFeature() {
		assertSame(screen2,
				Traversal.list("screens", e -> "screen2".equals(TestHelper.getPrimitiveValue(e, "name"))).hop(application));
		assertNull(Traversal.list("screens", e -> "screen999".equals(getValue(e, "name"))).hop(application));
	}

	@Test
	void hierarchy() {
		assertEquals(Arrays.asList(application), EcoreHelper.hierarchy(application).collect(Collectors.toList()));
		assertEquals(Arrays.asList(screen1, application), EcoreHelper.hierarchy(screen1).collect(Collectors.toList()));
		assertEquals(Arrays.asList(button1a, screen1, application),
				EcoreHelper.hierarchy(button1a).collect(Collectors.toList()));
		assertEquals(Arrays.asList(button2a, screen2, application),
				EcoreHelper.hierarchy(button2a).collect(Collectors.toList()));
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
		assertSame(button1b, search(nameAttribute, "screen1", "btn1b").hop(application));
		assertNull(search(nameAttribute, "btn2b").hop(screen1));
	}
}
