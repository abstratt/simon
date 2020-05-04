package com.abstratt.simon.compiler.ecore;

import static com.abstratt.simon.compiler.ecore.EcoreHelper.findChildByAttributeValue;
import static com.abstratt.simon.compiler.ecore.EcoreHelper.getValue;
import static java.util.Arrays.stream;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;

public interface Traversal {
	/**
	 * Hops from the given object to another one.
	 * 
	 * @param context
	 * @return the destination object, or null, if a hop was not possible
	 */
	EObject hop(EObject context);

	default Stream<EObject> enumerate(EObject context) {
		return Optional.ofNullable(hop(context)).map(Stream::of).orElseGet(Stream::empty);
	}

	interface Multiple extends Traversal {
		Stream<EObject> enumerate(EObject context);

		default EObject hop(EObject context) {
			return enumerate(context).findFirst().orElse(null);
		}

		default Multiple filter(Predicate<EObject> predicate) {
			return context -> enumerate(context).filter(predicate);
		}

	}

	class DebuggedTraversal implements Traversal {
		private Traversal base;
		private String description;

		public DebuggedTraversal(String description, Traversal base) {
			super();
			this.description = description;
			this.base = base;
		}

		@Override
		public EObject hop(EObject context) {
			System.out.println(description + " - hopping from " + context);
			EObject result = base.hop(context);
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
	default Traversal then(Traversal another) {
		return first -> Optional.ofNullable(first).map(this::hop).map(it -> debug("then", it)).map(another::hop)
				.orElse(null);
	}

	default Traversal debugged(String description) {
		return new DebuggedTraversal(description, this);

	}

	static Traversal search(EAttribute nameAttribute, String... path) {
		return search(nameAttribute, 0, path);
	}

	static Traversal search(EAttribute nameAttribute, int offset, String... path) {
		if (offset >= path.length)
			return self();
		// the first element is either a child or the child of an ancestor
		// the rest of the elements are resolved as (potential) children
		Traversal findChild = childWithAttributeValued(nameAttribute, path[offset]);
		Traversal resolveFirst = bubbleUp(findChild);
		if (path.length == offset + 1)
			return resolveFirst;
		Traversal traverseRest = compose(stream(path, offset + 1, path.length - offset)
				.map(segment -> childWithAttributeValued(nameAttribute, segment)));
		return resolveFirst.then(traverseRest);
	}

	/**
	 * Creates a constant hop that always traverses to the given target object.
	 * 
	 * @param target
	 * @return
	 */
	static Traversal to(EObject target) {
		return source -> debug("to: ", target);
	}

	static EObject debug(String message, EObject result) {
		System.out.println(message + " -> " + result);
		return result;
	}

	static <EL extends Collection<EObject>> EL debug(String message, EL result) {
		System.out.println(message);
		return result;
	}

	static Traversal self() {
		return source -> source;
	}

	static Traversal container() {
		return source -> source.eContainer();
	}

	/**
	 * Composes the given hops, resulting in a hop that will result the object
	 * returned by the first successful alternative.
	 * 
	 * @param alternatives
	 * @return
	 */
	static Traversal any(Traversal... alternatives) {
		return any(stream(alternatives));
	}

	/**
	 * Composes the given hops, resulting in a hop that produce the object returned
	 * by the first successful alternative.
	 * 
	 * @param alternatives
	 * @return
	 */
	static Traversal any(Stream<Traversal> alternatives) {
		return (EObject context) -> debug("any",
				alternatives.map(it -> it.hop(context)).filter(Objects::nonNull).findFirst().orElse(null));
	}

	static Traversal feature(String name) {
		return context -> debug("feature (" + name + ")", (EObject) getValue(context, name));
	}

	static Traversal childWithAttributeValued(EAttribute attribute, Object value) {
		return context -> debug("childWithAttributeValued (" + attribute.getName() + ") == " + value,
				findChildByAttributeValue(context, attribute, value));
	}

	static Traversal indexedFeature(String name, int index) {
		return context -> Optional.ofNullable(((EList<EObject>) getValue(context, name)))
				.flatMap(list -> list.stream().skip(index).findFirst()).orElse(null);
	}

	static Traversal list(String name, Predicate<EObject> predicate) {
		return context -> Optional.ofNullable(((EList<EObject>) getValue(context, name)))
				.flatMap(list -> list.stream().filter(predicate).findFirst()).orElse(null);
	}

	static Multiple children() {
		return context -> context.eContents().stream();
	}

	static Traversal condition(Predicate<EObject> test) {
		return context -> test.test(context) ? context : null;
	}

	static Traversal named(String name) {
		return condition(it -> EcoreHelper.hasName(it, name));
	}

	static Traversal root() {
		return EcoreUtil::getRootContainer;
	}

	/**
	 * Returns a hop that will produce the value of feature for its container.
	 * 
	 * @param name
	 * @return
	 */
	static Traversal containerFeature(String name) {
		return context -> debug("containerFeature(" + name + ")", (EObject) getValue(context.eContainer(), name));
	}

	/**
	 * Returns a hop that is equivalent to performing a sequence of hops.
	 */
	static Traversal compose(Stream<Traversal> hops) {
		return hops.reduce(e -> e, Traversal::then);
	}

	static Traversal compose(Traversal... hops) {
		return compose(stream(hops));
	}

	static Traversal bubbleUp(Traversal base) {
		return scope -> EcoreHelper.hierarchy(scope).map(t -> debug("bubbling up " + t, base.hop(t)))
				.filter(Objects::nonNull).findFirst().orElse(null);
	}
}