package com.abstratt.simon.metamodel.ecore.impl;

import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;

/**
 * A resolution context.
 * 
 * Resolution contexts are created to perform the mapping of a Java element to
 * an ECore element.
 */
public class MappingSession {

    @FunctionalInterface
    public interface ElementBuilder<EC extends ENamedElement> {
        EC build(MappingSession mappingSession, Class<?> clazz);

        default EPackage packageSelected(MappingSession mappingSession, Class<?> clazz) {
            throw new UnsupportedOperationException();
        }
    }

    public class SimpleElementBuilder<EC extends ENamedElement> implements ChildElementBuilder<EC> {
        private final EPackage scope;
        private final ElementBuilder<EC> baseBuilder;

        public SimpleElementBuilder(EPackage scope, ElementBuilder<EC> baseBuilder) {
            assert scope != null;
            this.scope = scope;
            this.baseBuilder = baseBuilder;
        }

        @Override
        public EC build(MappingSession mappingSession, Class<?> clazz) {
            return baseBuilder.build(mappingSession, clazz);
        }

        @Override
        public EPackage packageSelected(MappingSession mappingSession, Class<?> clazz) {
            return scope;
        }
    }

    public interface RootElementBuilder extends ElementBuilder<EPackage> {
    }

    public interface ChildElementBuilder<EC extends ENamedElement> extends ElementBuilder<EC> {
        @Override
        default EPackage packageSelected(MappingSession mappingSession, Class<?> clazz) {
            Class<?> packageClass = clazz.getEnclosingClass();
            while (packageClass != null && packageClass.getEnclosingClass() != null)
                packageClass = packageClass.getEnclosingClass();
            if (packageClass == null)
                return null;
            var ePackage = mappingSession.built.get(packageClass);
            return (EPackage) ePackage;
        }
    }

    /**
     * A delayed request is a request to perform an action asynchronously.
     * 
     * This is often required if the state the action to perform is not expected to
     * be available at the time the request is made,
     */
    private static class DelayedRequest {
        private final EPackage parent;
        private final Action action;
        private final String identifier;

        public DelayedRequest(Supplier<String> identifierSupplier, EPackage parent, Action action) {
            this.identifier = identifierSupplier.get();
            this.parent = parent;
            this.action = action;
        }

        @Override
        public String toString() {
            return "DelayedRequest [identifier=" + identifier + ", parent=" + parent + ", action=" + action + "]";
        }

    }

    public interface ContextConsumer extends Consumer<MappingSession> {
    }

    public interface Action {
        boolean isRoot();

        String description();

        void run(MappingSession context);
    }

    public static class SimpleAction implements Action {
        private final String description;
        private final ContextConsumer consumer;
        private final boolean root;

        public SimpleAction(String description, boolean root, ContextConsumer consumer) {
            this.description = description;
            this.root = root;
            this.consumer = consumer;
        }

        @Override
        public String description() {
            return description;
        }

        @Override
        public boolean isRoot() {
            return root;
        }

        @Override
        public void run(MappingSession context) {
            consumer.accept(context);
        }

        @Override
        public String toString() {
            return "SimpleAction [description=" + description + ", root=" + root + "]";
        }

    }

    /** The set of objects being built in this context. */
    private final Set<Object> building = new LinkedHashSet<>();
    private final Set<BuildAction<?>> served = new LinkedHashSet<>();
    private final Deque<MappingSession.DelayedRequest> pendingRequests = new LinkedList<>();
    private final Map<Class<?>, ENamedElement> built = new LinkedHashMap<>();
    private final AtomicInteger counter = new AtomicInteger();
    /**
     * Keeps a stack of current packages.
     */
    private final Deque<EPackage> currentPackage = new LinkedList<>();

    private Resource resource;

    public MappingSession() {
        resource = new ResourceImpl();
    }

    /**
     * Requests a Java type to be resolved to a model element.
     *
     * @param requestId      that Java element that we are mapping and needs
     *                       resolution, or null for the class itself
     * @param classToResolve The Java class we need to resolve to a model element
     * @param builder        a builder function that can, if necessary, map the Java
     *                       type to a model element
     * @param consumer       a function that will process the resulting model
     *                       element
     */
    public <EC extends ENamedElement> void mapChild(Class<?> classToResolve, ChildElementBuilder<EC> builder,
            Consumer<EC> consumer) {
        int requestId = counter.incrementAndGet();
        // we are not resolving for an element that needs the type, so use a unique id
        mapChild(requestId, classToResolve, builder, consumer);
    }

    /**
     * Requests a Java type to be resolved to a model element.
     *
     * @param requestId      that Java element that we are mapping and needs
     *                       resolution, or null for the class itself
     * @param classToResolve The Java class we need to resolve to a model element
     * @param builder        a builder function that can, if necessary, map the Java
     *                       type to a model element
     * @param consumer       a function that will process the resulting model
     *                       element
     */
    public <EC extends ENamedElement> void mapChild(Object optionalRequestId, Class<?> classToResolve,
            ChildElementBuilder<EC> builder, Consumer<EC> consumer) {
        map(new BuildAction<>(optionalRequestId, classToResolve, consumer, builder, false));
    }

    private boolean missingPackage(ChildElementBuilder<?> builder, Class<?> classToResolve) {
        return builder.packageSelected(this, classToResolve) == null;

    }

    public void mapRoot(Object optionalRequestId, Class<?> classToResolve, RootElementBuilder builder,
            Consumer<EPackage> consumer) {
        map(new BuildAction<>(optionalRequestId, classToResolve, consumer, builder, true));
    }

    public <EC extends ENamedElement> void mapUnder(EPackage ePackage, Object optionalRequestId,
            Class<?> classToResolve, ElementBuilder<EC> builder, Consumer<EC> consumer) {
        mapChild(optionalRequestId, classToResolve, new SimpleElementBuilder<EC>(ePackage, builder), consumer);
    }

    private <EC extends ENamedElement> void map(BuildAction<EC> task) {
        // System.out.println("Accepted request for " + task.requestTag);
        EC existing = (EC) built.get(task.classToResolve);
        if (existing != null) {
            // already resolved in this context, just serve it!
            serve(task, existing);
            return;
        }
        // Map the type asynchronously
        // System.out.println("Scheduling request " + task.requestTag);
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
    private <EC extends ENamedElement> void serve(BuildAction<EC> toServe, EC resolved) {
        if (served.add(toServe)) {
            // System.out.println("Serving request " + toServe);
            toServe.consumer.accept(resolved);
        } else {
            // System.out.println("Ignoring request " + toServe);
        }
    }

    public void processPendingRequests() {
        while (hasPendingRequests()) {
            MappingSession.DelayedRequest request = pendingRequests.removeFirst();
            if (request.action.isRoot())
                runAction(request.action);
            else
                runWithPackage(request.parent, request.action);
        }
    }

    public interface DescribedRunnable extends Runnable {
        default String description() {
            return null;
        }

        static DescribedRunnable describe(Supplier<String> describer, Runnable runnable) {
            return new DescribedRunnable() {

                @Override
                public void run() {
                    runnable.run();
                }

                @Override
                public String description() {
                    return describer.get();
                }
            };
        }
    }

    /**
     * Runs the given action within the package of the given parent element.
     * 
     * @param parent
     * @param action
     */
    public void runWithPackage(EPackage parent, Action action) {
        var runnable = DescribedRunnable.describe(action::description, () -> runAction(action));
        runWithPackage(parent, runnable);
    }

    public void runWithPackage(EPackage parent, DescribedRunnable runnable) {
        assert parent != null;
        enterPackage(parent);
        System.out.println("Running with package " + parent.getName() + ": " + runnable.description());
        try {
            runnable.run();
        } finally {
            leavePackage();
        }
        System.out.println("Completed running with package " + parent.getName() + ": " + runnable.description());
    }

    public void runWithPackage(EPackage parent, String description, ContextConsumer action) {
        Objects.requireNonNull(parent);
        runWithPackage(parent, new SimpleAction(description, false, action));
    }

    private void runAction(Action action) {
        action.run(this);
    }

    /**
     * Adds a new resolution request, to be resolved on the next call to
     * {@link #processPendingRequests()}.
     * 
     * A resolution request has the following components: - the parent Ecore
     * package, or null
     * 
     * 
     * @param parent     the parent package
     * @param identifier
     * @param request
     */
    public void addPendingRequest(EPackage parent, Supplier<String> identifier, Action request) {
        pendingRequests.add(new DelayedRequest(identifier, parent, request));
    }

    public void addPendingRequest(Supplier<String> identifier, Action request) {
        addPendingRequest(currentPackage(false), identifier, request);
    }

    public void addPendingRequest(EPackage parent, Supplier<String> identifier, String description, boolean isRoot,
            ContextConsumer consumer) {
        addPendingRequest(parent, identifier, new SimpleAction(identifier + " - " + description, isRoot, consumer));
    }

    public void addPendingRequest(Supplier<String> identifier, String description, boolean isRoot,
            ContextConsumer consumer) {
        addPendingRequest(currentPackage(!isRoot), identifier, description, isRoot, consumer);
    }

    public EPackage currentPackage() {
        return currentPackage(false);
    }

    public EPackage currentPackage(boolean required) {
        var currentPackageOrNull = currentPackage.peek();
        assert !required || currentPackageOrNull != null;
        return currentPackageOrNull;
    }

    public boolean inPackage() {
        return !currentPackage.isEmpty();
    }

    public void enterPackage(EPackage package_) {
        currentPackage.push(package_);
    }

    public void leavePackage() {
        currentPackage.pop();
    }

    /**
     * A build action is an action that converts a Java type into a corresponding
     * Ecore named element.
     * 
     * <ol>
     * <li>if the action has already been processed earlier, it is ignored
     * <li>If the Java type is in the process of being converted in the current
     * context, the request is submitted for being processed again at a later time
     * <li>If the Java type has already been converted in the current context, the
     * already built named element is served to the consumer
     * <li>Otherwise, the Java class is mapped to the corresponding named element by
     * running the given builder, and registered in the current context for later
     * reuse.
     * </ol>
     *
     * @param <EC>
     */
    private class BuildAction<EC extends ENamedElement> implements Action {
        private final Object requestId;
        private final String requestTag;
        private final Class<?> classToResolve;
        private final Consumer<EC> consumer;
        private final ElementBuilder<EC> builder;
        private final boolean isRoot;

        /**
         * @param optionalRequestId that Java element that we are mapping and needs
         *                          resolution, or null for the class itself
         * @param classToResolve    The Java class we need to resolve to a model element
         * @param builder           a builder function that can, if necessary, map the
         *                          Java type to a model element
         * @param consumer          a function that will process the resulting model
         * @param isRoot            whether the metaclass to resolve is a root metaclass
         */
        private BuildAction(Object optionalRequestId, Class<?> classToResolve, Consumer<EC> consumer,
                ElementBuilder<EC> builder, boolean isRoot) {
            this.requestId = optionalRequestId == null ? classToResolve : optionalRequestId;
            this.requestTag = requestId + " -> " + classToResolve.getSimpleName();
            this.classToResolve = classToResolve;
            this.consumer = consumer;
            this.builder = builder;
            this.isRoot = isRoot;
        }

        @Override
        public String description() {
            return requestTag;
        }

        @Override
        public boolean isRoot() {
            return isRoot;
        }

        private void resolve(MappingSession context) {
            // System.out.println("Async handling request " + requestTag);
            if (context.isServed(this)) {
                // We accept multiple requests, but only honor the first time
                // System.out.println("Already served: " + this);
                return;
            }
            EC existingElement = (EC) context.built.get(classToResolve);
            if (existingElement != null) {
                // System.out.println("Already solved: " + this);
                context.serve(this, existingElement);
                return;
            }
            boolean isNewRequest = context.building.add(classToResolve);
            if (!isNewRequest) {
                // already building - just reschedule so we will serve it when done
                // System.out.println("Re-scheduling request " + this);
                context.addPendingRequest(context.currentPackage(), requestTag::toString, this);
                return;
            }
            // TODO-RC we need to run the following under the proper EPackage
            // actually build the element
            // System.out.println("Building for " + this);
            EC newElement = builder.build(context, classToResolve);
            if (isRoot)
                resource.getContents().add(newElement);
            context.built.put(classToResolve, newElement);
            System.out.println("Built for " + this);
            context.serve(this, newElement);
        }

        @Override
        public void run(MappingSession context) {
            assert context == MappingSession.this;
            if (!isRoot) {
                var selectedPackage = builder.packageSelected(context, classToResolve);
                System.out.println("selectedPackage for " + classToResolve.getName() + " -> "
                        + Optional.ofNullable(selectedPackage).map(EPackage::getName));
                if (selectedPackage == null)
                    resolve(context);
                else
                    context.runWithPackage(selectedPackage,
                            DescribedRunnable.describe(this::description, () -> resolve(context)));
            } else
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