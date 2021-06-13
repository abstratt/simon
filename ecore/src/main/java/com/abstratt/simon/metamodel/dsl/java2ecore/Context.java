package com.abstratt.simon.metamodel.dsl.java2ecore;

import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.emf.ecore.ENamedElement;

/**
 * A resolution context.
 * 
 * Resolution contexts are created to perform the mapping of a Java element to
 * an ECore element.
 */
public class Context {
	private static class DelayedRequest {
		private final ENamedElement parent;
		private final Action action;
		private String identifier;

		public DelayedRequest(Supplier<String> identifierSupplier, ENamedElement parent, Action action) {
			this.identifier = identifierSupplier.get();
			this.parent = parent;
			this.action = action;
		}

		@Override
		public String toString() {
			return "DelayedRequest [identifier=" + identifier + ", parent=" + parent + ", action=" + action + "]";
		}

	}

	public interface ContextConsumer extends Consumer<Context> {
	}

	public interface Action {
		boolean isRoot();

		void run(Context context);
	}

	public static class SimpleAction implements Action {
		private String description;
		private ContextConsumer consumer;
		private boolean root;

		public SimpleAction(String description, boolean root, ContextConsumer consumer) {
			this.description = description;
			this.root = root;
			this.consumer = consumer;
		}

		@Override
		public boolean isRoot() {
			return root;
		}

		@Override
		public void run(Context context) {
			consumer.accept(context);
		}

		@Override
		public String toString() {
			return "SimpleAction [description=" + description + ", root=" + root + "]";
		}

	}

	/** The set of objects being built in this context. */
	private Set<Object> building = new LinkedHashSet<>();
	private Set<ResolutionAction<?>> served = new LinkedHashSet<>();
	private Deque<Context.DelayedRequest> pendingRequests = new LinkedList<>();
	private Map<Class<?>, ENamedElement> built = new LinkedHashMap<>();
	private AtomicInteger counter = new AtomicInteger();
	/**
	 * Keeps a stack of current packages.
	 */
	Deque<ENamedElement> currentScope = new LinkedList<>();

	/**
	 * Requests a type to be resolved in the context of mapping a Java element to a
	 * model element.
	 *
	 * @param requestId      that Java element that we are mapping and needs
	 *                       resolution, or null for the class itself
	 * @param classToResolve The Java class we need to resolve to a model element
	 * @param builder        a builder function that can, if necessary, map the Java
	 *                       type to a model element
	 * @param consumer       a function that will process the resulting model
	 *                       element
	 */
	public <EC extends ENamedElement> void resolve(Class<?> type, BiFunction<Context, Class<?>, EC> builder,
			Consumer<EC> consumer) {
		int requestId = counter.incrementAndGet();
		// we are not resolving for an element that needs the type, so use a unique id
		this.resolve(requestId, type, builder, consumer);
	}

	/**
	 * Requests a type to be resolved in the context of mapping a Java element to a
	 * model element.
	 *
	 * @param requestId      that Java element that we are mapping and needs
	 *                       resolution, or null for the class itself
	 * @param classToResolve The Java class we need to resolve to a model element
	 * @param builder        a builder function that can, if necessary, map the Java
	 *                       type to a model element
	 * @param consumer       a function that will process the resulting model
	 *                       element
	 */
	public <EC extends ENamedElement> void resolve(Object optionalRequestId, Class<?> classToResolve,
			BiFunction<Context, Class<?>, EC> builder, Consumer<EC> consumer) {
		resolve(new ResolutionAction<>(optionalRequestId, classToResolve, consumer, builder, false));
	}

	public <EC extends ENamedElement> void resolveRoot(Object optionalRequestId, Class<?> classToResolve,
			BiFunction<Context, Class<?>, EC> builder, Consumer<EC> consumer) {
		resolve(new ResolutionAction<>(optionalRequestId, classToResolve, consumer, builder, true));
	}

	public <EC extends ENamedElement> void resolve(ResolutionAction<EC> task) {
		System.out.println("Accepted request for " + task.requestTag);
		EC existing = (EC) built.get(task.classToResolve);
		if (existing != null) {
			// already resolved in this context, just serve it!
			serve(task, existing);
			return;
		}
		// Map the type asynchronously
		System.out.println("Scheduling request " + task.requestTag);
		addPendingRequest(task.requestTag::toString, task);
	}

	private boolean isServed(Action action) {
		return served.contains(action);
	}

	/**
	 * Serves a request for an object, invoking the consumer.
	 * 
	 * If the request has already been served in this context, the consumer will not
	 * be invoked again.
	 * 
	 * @param <EC>      the type of element resolved
	 * @param requestId the id of the request to be served
	 * @param consumer  the consumer for the element resolved
	 * @param resolved  the resolved element
	 */
	private <EC extends ENamedElement> void serve(ResolutionAction<EC> toServe, EC resolved) {
		if (served.add(toServe)) {
			System.out.println("Serving request " + toServe);
			toServe.consumer.accept(resolved);
		} else {
			System.out.println("Ignoring request " + toServe);
		}
	}

	public void resolvePendingRequests() {
		while (hasPendingRequests()) {
			Context.DelayedRequest request = pendingRequests.removeFirst();
			if (request.action.isRoot())
				runAction(request.action);
			else
				runWithScope(request.parent, request.action);
		}
	}

	/**
	 * Runs the given action within the scope of the given parent element.
	 * 
	 * @param parent
	 * @param action
	 */
	public void runWithScope(ENamedElement parent, Action action) {
		assert parent != null;
		enterScope(parent);
		try {
			runAction(action);
		} finally {
			leaveScope();
		}

	}

	public void runWithScope(ENamedElement parent, String description, ContextConsumer action) {
		Objects.requireNonNull(parent);
		assert parent != null : description;
		runWithScope(parent, new SimpleAction(description, false, action));
	}

	private void runAction(Action action) {
		action.run(this);
	}

	/**
	 * Adds a new resolution request, to be resolved on the next call to
	 * {@link #resolvePendingRequests()}.
	 * 
	 * @param identifier
	 * @param request
	 */
	public void addPendingRequest(Supplier<String> identifier, Action request) {
		ENamedElement parent = currentScope();
		assert parent != null || request.isRoot() : identifier.get() + " - " + request;
		pendingRequests.add(new DelayedRequest(identifier, parent, request));
	}

	public void addPendingRequest(Supplier<String> identifier, String description, boolean isRoot,
			ContextConsumer consumer) {
		addPendingRequest(identifier, new SimpleAction(identifier + " - " + description, isRoot, consumer));
	}

	public ENamedElement currentScope() {
		return currentScope.peek();
	}

	public void enterScope(ENamedElement package_) {
		currentScope.push(package_);
	}

	public void leaveScope() {
		currentScope.pop();
	}

	public static class ResolutionAction<EC extends ENamedElement> implements Action {
		private final Object requestId;
		private final String requestTag;
		private final Class<?> classToResolve;
		private final Consumer<EC> consumer;
		private final BiFunction<Context, Class<?>, EC> builder;
		private boolean isRoot;

		/**
		 * @param optionalRequestId that Java element that we are mapping and needs
		 *                          resolution, or null for the class itself
		 * @param classToResolve    The Java class we need to resolve to a model element
		 * @param builder           a builder function that can, if necessary, map the
		 *                          Java type to a model element
		 * @param consumer          a function that will process the resulting model
		 */
		public ResolutionAction(Object optionalRequestId, Class<?> classToResolve, Consumer<EC> consumer,
				BiFunction<Context, Class<?>, EC> builder, boolean isRoot) {
			this.requestId = optionalRequestId == null ? classToResolve : optionalRequestId;
			this.requestTag = requestId + " -> " + classToResolve.getSimpleName();
			this.classToResolve = classToResolve;
			this.consumer = consumer;
			this.builder = builder;
			this.isRoot = isRoot;
		}

		@Override
		public boolean isRoot() {
			return isRoot;
		}

		void resolve(Context context) {
			System.out.println("Async handling request " + requestTag);
			if (context.isServed(this)) {
				// We accept multiple requests, but only honor the first time
				System.out.println("Already served: " + this);
				return;
			}
			EC existingElement = (EC) context.built.get(classToResolve);
			if (existingElement != null) {
				System.out.println("Already solved: " + this);
				context.serve(this, existingElement);
				return;
			}
			boolean isNewRequest = context.building.add(classToResolve);
			if (!isNewRequest) {
				// already building - just reschedule so we will serve it when done
				System.out.println("Re-scheduling request " + this);
				context.addPendingRequest(requestTag::toString, this);
				return;
			}
			// actually build the element
			System.out.println("Building for " + this);
			EC newElement = builder.apply(context, classToResolve);
			context.built.put(classToResolve, newElement);
			System.out.println("Built for " + this);
			context.serve(this, newElement);

		}

		@Override
		public void run(Context context) {
			resolve(context);
		}

		@Override
		public String toString() {
			return "ResolutionAction [requestId=" + requestId + ", requestTag=" + requestTag + ", classToResolve="
					+ classToResolve + ", isRoot=" + isRoot + "]";
		}

	}

	public boolean hasPendingRequests() {
		return !pendingRequests.isEmpty();
	}

}