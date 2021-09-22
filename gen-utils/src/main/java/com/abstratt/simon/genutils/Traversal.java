package com.abstratt.simon.genutils;

import static java.util.Arrays.stream;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface Traversal<T> {
    /**
     * Hops from the given object to another one.
     * 
     * @param context
     * @return the destination object, or null, if a hop was not possible
     */
    T hop(T context);

    default Stream<T> enumerate(T context) {
        return Optional.ofNullable(hop(context)).stream();
    }

    static <T> T debug(String message, T result) {
        // System.out.println(message + " -> " + result);
        return result;
    }

    static <T, EL extends Collection<T>> EL debug(String message, EL result) {
        // System.out.println(message);
        return result;
    }

    interface Provider<T, F> {
        Traversal<T> root();

        Traversal<T> named(String name);

        Traversal<T> container();

        Multiple<T> children();

        Multiple<T> roots();

        Traversal<T> childWithAttributeValued(F attribute, Object value);

        boolean hasAttributeValued(T context, F attribute, Object value);

        String featureName(F feature);

        /**
         * Returns a hop that will produce the value of feature for its container.
         * 
         * @param name
         *
         * @return
         */
        Traversal<T> containerFeature(String name);

        /**
         * A traversal that attempts to perform the given traversal in the scope or any
         * parent scope.
         *
         * @param base the base traversal
         * @return a traversal that bubbles up the scope hierarchy
         */
        default Traversal<T> bubbleUp(Traversal<T> base) {
            return scope -> hierarchy(scope).map(t -> debug("bubbling up " + t, base.hop(t))).filter(Objects::nonNull)
                    .findFirst().orElse(null);
        }

        default Traversal<T> attributeValued(F attribute, Object value) {
            return condition(context -> hasAttributeValued(context, attribute, value));
        }

        /**
         * Returns the hierarchy of contexts starting from the given context.
         *
         * @param context starting context
         * @return the aggregation chain for the given context
         */
        Stream<T> hierarchy(T context);

        <O> O getValue(T eObject, String featureName);

        /**
         * Creates a constant hop that always traverses to the given target object.
         * 
         * @param target
         * @return
         */
        default Traversal<T> to(T target) {
            return source -> debug("to: ", target);
        }

        default Traversal<T> self() {
            return source -> source;
        }

        /**
         * Composes the given hops, resulting in a hop that will result the object
         * returned by the first successful alternative.
         * 
         * @param alternatives
         * @return
         */
        default Traversal<T> any(Traversal<T>... alternatives) {
            return (T context) -> debug("any",
                    stream(alternatives).map(it -> it.hop(context)).filter(Objects::nonNull).findFirst().orElse(null));
        }

        default Traversal<T> search(F feature, Object... path) {
            return search(feature, 0, path);
        }

        /**
         * Builds a traversal that tries to find an object that has the given value(s).
         *
         * The traversal attempts to find a base object following the following
         * strategy:
         * <ol>
         * <li>a child in the current context that has the given feature (attribute or
         * relationship) with the first value given</li>
         * <li>if no child is found with the given value for the given feature, attempt
         * the first step having each of the parent scopes as context</li>
         * <li>for each additional value to match, the child with the given
         * feature.</li>
         * </ol>
         *
         * @param feature feature to match
         * @param offset  values to skip
         * @param path    the path of values to match
         * @return the search traversal
         */
        default Traversal<T> search(F feature, int offset, Object... path) {
            if (offset >= path.length)
                return self();
            // the first value matches either:
            // 1. a direct child
            // 2. an ancestor
            // 3. the direct child of an ancestor
            // 4. any root
            var findParentOrRelative = any(attributeValued(feature, path[offset]),
                    childWithAttributeValued(feature, path[offset]));
            var resolveFirst = bubbleUp(findParentOrRelative);
            if (path.length == offset + 1)
                // if no more values to match, we stop here
                return resolveFirst;
            var traverseRest = compose(stream(path, offset + 1, path.length - offset)
                    .map(segment -> childWithAttributeValued(feature, segment)));
            var localLookup = resolveFirst.then(traverseRest);
            // TODO-RC this will search again the current tree unnecessarily
            var globalSearch = roots().then(traverseRest);
            return any(localLookup, globalSearch);
        }

        default Traversal<T> feature(String name) {
            return context -> debug("feature (" + name + ")", (T) getValue(context, name));
        }

        default Traversal<T> indexedFeature(String name, int index) {
            return context -> Optional.ofNullable(((List<T>) getValue(context, name)))
                    .flatMap(list -> list.stream().skip(index).findFirst()).orElse(null);
        }

        default Traversal<T> list(String name, Predicate<T> predicate) {
            return context -> Optional.ofNullable(((List<T>) getValue(context, name)))
                    .flatMap(list -> list.stream().filter(predicate).findFirst()).orElse(null);
        }

        default Traversal<T> condition(Predicate<T> test) {
            return context -> test.test(context) ? context : null;
        }

        default Traversal<T> withValue(F feature, Object value) {
            return condition((T scope) -> hasAttributeValued(scope, feature, value));
        }

        /**
         * Returns a hop that is equivalent to performing a sequence of hops.
         */
        default Traversal<T> compose(Stream<Traversal<T>> hops) {
            return hops.reduce(e -> e, Traversal::then);
        }

        default Traversal<T> compose(Traversal<T>... hops) {
            return compose(stream(hops));
        }
    }

    interface Multiple<T> extends Traversal<T> {
        Stream<T> enumerate(T context);

        default T hop(T context) {
            return enumerate(context).filter(Objects::nonNull).findFirst().orElse(null);
        }

        default Multiple<T> filter(Predicate<T> predicate) {
            return context -> enumerate(context).filter(predicate);
        }

        @Override
        default Multiple<T> then(Traversal<T> another) {
            return (T first) -> enumerate(first).filter(Objects::nonNull).map(another::hop);
        }
    }

    class DebuggedTraversal<T> implements Traversal<T> {
        private final Traversal<T> base;
        private final String description;

        public DebuggedTraversal(String description, Traversal<T> base) {
            this.description = description;
            this.base = base;
        }

        @Override
        public T hop(T context) {
            System.out.println(description + " - hopping from " + context);
            T result = base.hop(context);
            System.out.println(description + " - hopping from " + context + "\n\tresult: " + result);
            return result;
        }

    }

    /**
     * Composes this hop and another hop so the result is the same as hop2(hop1(c)).
     * 
     * @param another
     * @return
     */
    default Traversal<T> then(Traversal<T> another) {
        return (T first) -> Optional.ofNullable(first).map(this::hop).map(it -> debug("then", it)).map(another::hop)
                .orElse(null);
    }

    default Traversal<T> debugged(String description) {
        return new DebuggedTraversal<>(description, this);

    }

}