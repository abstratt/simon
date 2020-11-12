package com.abstratt.simon.compiler.ecore;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcoreFactory;

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
	
	public static EStructuralFeature findStructuralFeature(EClass eClass, String featureName) {
		return findInList(eClass.getEAllStructuralFeatures(), feature -> feature.getName().equals(featureName));
	}
	
	public static EObject findByFeature(Collection<EObject> elements, String featureName, Object value) {
		return findInList(elements, e -> value.equals(getValue(e, featureName)));
	}

	public static <T> T findInList(Collection<T> collection,
			Predicate<T> predicate) {
		return collection.stream().filter(predicate).findAny().orElse(null);
	}
	
	public static boolean hasAttributeValue(EObject toCheck, EAttribute feature, Object value) {
		return value.equals(toCheck.eGet(feature));
	}
	
	public static EObject findChildByAttributeValue(EObject toCheck, EAttribute feature, Object value) {
		return toCheck.eContents().stream().filter(e -> hasAttributeValue(e, feature, value)).findFirst().orElse(null);
	}
	
	public static EStructuralFeature findStructuralFeature(EObject eObject, String featureName) {
		return findStructuralFeature(eObject.eClass(), featureName);
	}
	
	public static <F extends EStructuralFeature> F findFeatureInHierarchy(EObject scope, String featureName) {
		return (F) EcoreHelper.hierarchy(scope).map(e -> EcoreHelper.findStructuralFeature(e, featureName)).filter(Objects::nonNull).findFirst().orElseThrow(() -> new IllegalArgumentException("No 'name' feature found in composition structure"));
	}

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
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                iterator,
                Spliterator.ORDERED | Spliterator.IMMUTABLE), false);
	}


}
