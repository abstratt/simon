package com.abstratt.simon.java2ecore;

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
import org.eclipse.emf.ecore.EcorePackage;
import org.junit.jupiter.api.Test;

import com.abstratt.simon.metamodel.ecore.java2ecore.Context;

public class ContextTest {
	@Test
	void pendingRequests() {
		Context context = new Context();
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
		context.resolvePendingRequests();
		assertEquals(asList("1", "2", "3", "2b", "3b", "3c"), log);
		log.clear();
		context.resolvePendingRequests();
		assertEquals(emptyList(), log);
	}

	@Test
	void runWithScope() {
		EcorePackage someElement = EcorePackage.eINSTANCE;
		Context context = new Context();
		List<ENamedElement> collected = new ArrayList<>();
		context.runWithScope(someElement, "", ctx -> collected.add(ctx.currentScope()));
		assertEquals(asList(someElement), collected);
	}

	@Test
	void runWithScopeRequiresScope() {
		Context context = new Context();
		assertNull(context.currentScope());
		List<ENamedElement> collected = new ArrayList<>();
		try {
			context.runWithScope(null, "", ctx -> collected.add(ctx.currentScope()));
			fail();
		} catch (NullPointerException e) {
			// expected
		}
		assertEquals(emptyList(), collected);
	}

	@Test
	void resolve() {
		EcorePackage someElement = EcorePackage.eINSTANCE;
		Class<ContextTest> someClass = ContextTest.class;
		Context context = new Context();
		List<EcorePackage> collected = new ArrayList<>();
		Consumer<EcorePackage> consumer = built -> collected.add(built);
		BiFunction<Context, Class<?>, EcorePackage> builder = (ctx, clazz) -> EcorePackage.eINSTANCE;
		context.resolve(new Context.ResolutionAction<>("id1", someClass, consumer, builder, true));
		assertEquals(emptyList(), collected);
		assertTrue(context.hasPendingRequests());

		context.resolvePendingRequests();
		assertEquals(asList(someElement), collected);

		// second attempt should get the same model element without rebuilding
		context.resolve(new Context.ResolutionAction<>("id2", someClass, consumer,
				(ctx, clazz) -> fail("Should not attempt to rebuild for a same class"), true));
		assertTrue(!context.hasPendingRequests());
		assertEquals(asList(someElement, someElement), collected);
	}

}
