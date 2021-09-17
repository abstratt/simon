package com.abstratt.simon.tests;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.junit.jupiter.api.Test;

import com.abstratt.simon.metamodel.ecore.impl.MappingSession;
import com.abstratt.simon.metamodel.ecore.impl.MappingSession.ElementBuilder;
import com.abstratt.simon.metamodel.ecore.impl.MappingSession.RootElementBuilder;

public class ContextTest {
	@Test
	void pendingRequests() {
		MappingSession context = new MappingSession();
		List<String> log = new ArrayList<>();
		context.addPendingRequest("request 1"::toString, "", true, ctx -> log.add("1"));
		context.addPendingRequest("request 2"::toString, "", true, ctx -> {
			log.add("2");
			context.addPendingRequest("request 2"::toString, "", true, ctx2 -> log.add("2b"));
			context.addPendingRequest("request 3"::toString, "", true, ctx2 -> log.add("3b"));
		});
		context.addPendingRequest("request 3"::toString, "", true, ctx -> {
			log.add("3");
			context.addPendingRequest("request 3"::toString, "", true, ctx2 -> log.add("3c"));
		});
		assertEquals(emptyList(), log);
		context.processPendingRequests();
		assertEquals(asList("1", "2", "3", "2b", "3b", "3c"), log);
		log.clear();
		context.processPendingRequests();
		assertEquals(emptyList(), log);
	}

	@Test
	void runWithScope() {
		EcorePackage someElement = EcorePackage.eINSTANCE;
		MappingSession context = new MappingSession();
		List<ENamedElement> collected = new ArrayList<>();
		context.runWithPackage(someElement, "", ctx -> collected.add(ctx.currentPackage()));
		assertEquals(asList(someElement), collected);
	}

	@Test
	void runWithScopeRequiresScope() {
		MappingSession context = new MappingSession();
		assertNull(context.currentPackage());
		List<ENamedElement> collected = new ArrayList<>();
		try {
			context.runWithPackage(null, "", ctx -> collected.add(ctx.currentPackage()));
			fail();
		} catch (NullPointerException e) {
			// expected
		}
		assertEquals(emptyList(), collected);
	}

	@Test
	void resolve() {
		EPackage someElement = EcoreFactory.eINSTANCE.createEPackage();
		Class<ContextTest> someClass = ContextTest.class;
		MappingSession context = new MappingSession();
		List<EPackage> collected = new ArrayList<>();
		Consumer<EPackage> consumer = collected::add;
		RootElementBuilder builder = (ctx, clazz) -> someElement;
		context.mapRoot("id1", someClass, builder, consumer);
		assertEquals(emptyList(), collected);
		assertTrue(context.hasPendingRequests());

		context.processPendingRequests();
		assertEquals(asList(someElement), collected);

		// second attempt should get the same model element without rebuilding
		context.mapRoot("id2", someClass,
				(ctx, clazz) -> fail("Should not attempt to rebuild for a same class"), 
				consumer);
		assertTrue(!context.hasPendingRequests());
		assertEquals(asList(someElement, someElement), collected);
	}

}
