package com.abstratt.simon.compiler.ecore;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.util.EcoreUtil;

import com.abstratt.simon.metamodel.dsl.java2ecore.MetaEcoreHelper;

public class EcoreHelper {

	private static final EObject NONE = EcoreFactory.eINSTANCE.createEObject();

	public static <O> O getValue(EObject eObject, String featureName) {
		EStructuralFeature feature = findStructuralFeature(eObject, featureName);
		return feature == null ? null : (O) eObject.eGet(feature);
	}

	public static <O> Stream<O> getValueAsStream(EObject eObject, String featureName) {
		EStructuralFeature feature = findStructuralFeature(eObject, featureName);
		return feature == null ? Stream.empty() : getValueAsStream(eObject, feature);
	}

	public static <O> Stream<O> getValueAsStream(EObject eObject, EStructuralFeature feature) {
		Object value = eObject.eGet(feature);
		Optional<Stream<O>> stream = feature.isMany()
				? Optional.ofNullable((Collection<O>) value).map(Collection::stream)
				: Optional.ofNullable((O) value).map(Stream::of);
		return stream.orElse(Stream.empty());
	}

	static void setName(EObject named, String name) {
		System.out.println("Setting name of a " + named.eClass().getName() + " to " + name);
		// TODO-RC what to do about objects that do not support naming? Is it supported
		// across the board?
		EStructuralFeature nameProperty = MetaEcoreHelper.getNameAttribute(named);
		if (nameProperty == null) {
			throw new IllegalArgumentException("No 'name' feature in '" + named.eClass().getName());
		}
		named.eSet(nameProperty, wrappedPrimitiveValue((EClass) nameProperty.getEType(), name));
	}

	static EObject wrappedPrimitiveValue(EClass eClass, Object value) {
		EObject wrapper = EcoreUtil.create(eClass);
		EStructuralFeature valueFeature = MetaEcoreHelper.getValueFeature(eClass);
		assert valueFeature != null : "No value feature in " + eClass;
		wrapper.eSet(valueFeature, value);
		return wrapper;
	}

	public static <O> O unwrappedPrimitiveValue(EObject primitiveValue) {
		if (primitiveValue == null)
			return null;
		EStructuralFeature valueFeature = MetaEcoreHelper.getValueFeature(primitiveValue.eClass());
		assert valueFeature != null : "No value feature in " + primitiveValue.eClass();
		return (O) primitiveValue.eGet(valueFeature);
	}

	public static EStructuralFeature findStructuralFeature(EClass eClass, String featureName) {
		return findInList(eClass.getEAllStructuralFeatures(), feature -> feature.getName().equals(featureName));
	}

	public static EObject findByFeature(Collection<EObject> elements, String featureName, Object value) {
		return findInList(elements, e -> value.equals(getValue(e, featureName)));
	}

	public static <T> T findInList(Collection<T> collection, Predicate<T> predicate) {
		return collection.stream().filter(predicate).findAny().orElse(null);
	}

	public static boolean hasAttributeValue(EObject toCheck, EAttribute feature, Object valueExpected) {
		Object actual = toCheck.eGet(feature);
		if (actual instanceof EObject)
			actual = unwrappedPrimitiveValue((EObject) actual);
		return valueExpected.equals(actual);
	}

	public static boolean hasPrimitiveAttributeValue(EObject toCheck, EAttribute feature, Object valueExpected) {
		Object actual = toCheck.eGet(feature);
		return valueExpected.equals(actual);
	}

	// TODO-RC - this and hasAttributeValue need to take into account string values
	// are wrapped!
	public static boolean hasName(EObject eObject, String name) {
		return hasAttributeValue(eObject, MetaEcoreHelper.getNameAttribute(eObject), name);
	}

	public static EObject findChildByAttributeValue(EObject toCheck, EAttribute feature, Object value) {
		EList<EObject> eContents = toCheck.eContents();
		return eContents.stream().filter(e -> hasAttributeValue(e, feature, value)).findFirst().orElse(null);
	}

	public static EStructuralFeature findStructuralFeature(EObject eObject, String featureName) {
		return findStructuralFeature(eObject.eClass(), featureName);
	}

	public static EClassifier findClassifierByName(EPackage ePackage, String typeName) {
		return ePackage.getEClassifiers().stream().filter(it -> typeName.equalsIgnoreCase(it.getName())).findAny()
				.orElse(null);
	}

	public static <F extends EStructuralFeature> F findFeatureInHierarchy(EObject scope, String featureName) {
		return (F) EcoreHelper.hierarchy(scope).map(e -> EcoreHelper.findStructuralFeature(e, featureName))
				.filter(Objects::nonNull).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("No 'name' feature found in composition structure"));
	}

	public static Stream<EObject> tree(EObject start) {
		Stream<EObject> children = StreamSupport.stream(Spliterators.spliteratorUnknownSize(
				EcoreUtil.getAllContents(start, true), Spliterator.ORDERED | Spliterator.IMMUTABLE), false);
		return Stream.concat(Stream.of(start), children);
	}

	/**
	 * Returns all elements starting from the given object all the way up to its
	 * root container.
	 */
	public static Stream<EObject> hierarchy(EObject start) {
		final Iterator<EObject> iterator = new Iterator<EObject>() {
			@SuppressWarnings("unchecked")
			EObject t = NONE;

			@Override
			public boolean hasNext() {
				return t == NONE || t.eContainer() != null;
			}

			@Override
			public EObject next() {
				return t = (t == NONE) ? start : t.eContainer();
			}
		};
		return StreamSupport.stream(
				Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED | Spliterator.IMMUTABLE), false);
	}

}
