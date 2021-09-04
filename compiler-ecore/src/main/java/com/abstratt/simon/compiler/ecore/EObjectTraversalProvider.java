package com.abstratt.simon.compiler.ecore;

import java.util.stream.Stream;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;

import com.abstratt.simon.genutils.Traversal;
import com.abstratt.simon.genutils.Traversal.Multiple;
import com.abstratt.simon.metamodel.ecore.java2ecore.EcoreHelper;

public interface EObjectTraversalProvider extends Traversal.Provider<EObject, EAttribute> {
	default Multiple<EObject> roots() {
		return context -> {
			var resource = context.eResource();
			var roots = resource.getContents();
			Traversal.debug("roots: " + roots, roots);
			return roots.stream();
		};
	}

	@Override
	default Traversal<EObject> container() {
		return source -> source.eContainer();
	}
	@Override
	default Traversal<EObject> named(String name) {
		return condition(it -> EcoreHelper.hasName(it, name));
	}
	@Override
	default Traversal<EObject> root() {
		return EcoreUtil::getRootContainer;
	}
	@Override
	default Multiple<EObject> children() {
		return context -> context.eContents().stream();
	}
	@Override
	default String featureName(EAttribute feature) {
		return feature.getName();
	}
	@Override
	default Traversal<EObject> containerFeature(String name) {
		return context -> Traversal.debug("containerFeature(" + name + ")", (EObject) getValue(context.eContainer(), name));
	}
	
	@Override
	default <O> O getValue(EObject eObject, String featureName) {
		return EcoreHelper.getValue(eObject, featureName);
	}
	
	@Override
	default Stream<EObject> hierarchy(EObject context) {
		return EcoreHelper.hierarchy(context);
	}
	
	@Override
	default Traversal<EObject> childWithAttributeValued(EAttribute attribute, Object value) {
		return context -> Traversal.debug("childWithAttributeValued (" + attribute + ") == " + value,
				EcoreHelper.findChildByAttributeValue(context, attribute, value));
	}

	@Override
	default boolean hasAttributeValued(EObject context, EAttribute attribute, Object value) {
		return EcoreHelper.hasAttributeValue(context, attribute, value);
	}

	Traversal.Provider<EObject, EAttribute> INSTANCE = new ProviderImpl();
}


class ProviderImpl implements EObjectTraversalProvider {

}