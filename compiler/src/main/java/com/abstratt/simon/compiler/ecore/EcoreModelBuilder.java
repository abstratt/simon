package com.abstratt.simon.compiler.ecore;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;

import com.abstratt.simon.compiler.Configuration;
import com.abstratt.simon.compiler.Configuration.Instantiation;
import com.abstratt.simon.compiler.Configuration.Linking;
import com.abstratt.simon.compiler.Configuration.NameQuerying;
import com.abstratt.simon.compiler.Configuration.NameResolution;
import com.abstratt.simon.compiler.Configuration.NameSetting;
import com.abstratt.simon.compiler.Configuration.Parenting;
import com.abstratt.simon.compiler.Configuration.ValueSetting;
import com.abstratt.simon.compiler.ecore.EcoreMetamodel.EcoreObjectType;
import com.abstratt.simon.compiler.ecore.EcoreMetamodel.EcoreRelationship;
import com.abstratt.simon.compiler.ecore.EcoreMetamodel.EcoreSlot;
import com.abstratt.simon.compiler.ecore.EcoreMetamodel.EcoreSlotted;

public class EcoreModelBuilder implements Configuration.Provider<EcoreObjectType, EcoreSlotted<?>, EObject> {

	@Override
	public NameResolution<EObject> nameResolution() {
		return this::resolve;
	}

	@Override
	public Instantiation<EcoreSlotted<?>> instantiation() {
		return this::createObject;
	}

	@Override
	public Linking<EObject, EcoreRelationship> linking() {
		return this::link;
	}

	@Override
	public NameSetting<EObject> nameSetting() {
		return this::setName;
	}

	@Override
	public NameQuerying<EObject> nameQuerying() {
		return this::getName;
	}

	@Override
	public ValueSetting<EObject, EcoreSlot> valueSetting() {
		return this::setValue;
	}

	@Override
	public Parenting<EObject, EcoreRelationship> parenting() {
		return this::addChild;
	}

	public <E extends EObject> E createObject(EcoreSlotted<?> resolvedType) {
		return (E) resolvedType.newModelElement();
	}

	public EObject resolve(EObject scope, String... path) {
		EAttribute nameAttribute = EcoreHelper.findFeatureInHierarchy(scope, "name");
		EObject resolved = Traversal.search(nameAttribute, path).hop(scope);
		System.out.println("Resolved " + Arrays.asList(path) + " to " + Optional.ofNullable(resolved)
				.map(it -> getName(it) + " : " + resolved.eClass().getName()).orElse(null));
		return resolved;
	}

	public String getName(EObject named) {
		EStructuralFeature nameProperty = getNameAttribute(named);
		if (nameProperty == null) {
			return null;
		}
		return (String) named.eGet(nameProperty);
	}

	private EAttribute getNameAttribute(EObject named) {
		EClass eClass = named.eClass();
		return getNameAttribute(eClass);
	}

	private EAttribute getNameAttribute(EClass eClass) {
		return (EAttribute) eClass.getEStructuralFeature("name");
	}

	public void setName(EObject named, String name) {
		System.out.println("Setting name of a " + named.eClass().getName() + " to " + name);
		// TODO-RC what to do about objects that do not support naming? Is it supported
		// across the board?
		EStructuralFeature nameProperty = getNameAttribute(named);
		if (nameProperty == null) {
			throw new IllegalArgumentException("No 'name' feature in '" + named.eClass().getName());
		}
		named.eSet(nameProperty, name);
	}

	public void link(EcoreRelationship reference, EObject referrer, EObject referred) {
		setOrAddReference(referrer, referred, reference);
	}

	public void addChild(EcoreRelationship composition, EObject parent, EObject child) {
		setOrAddReference(parent, child, composition);
	}

	public void setParent(EcoreRelationship composition, EObject child, EObject parent) {
		child.eSet(composition.wrapped(), parent);
	}

	public void setValue(EcoreSlot slot, EObject target, Object value) {
		target.eSet(slot.wrapped(), value);
	}

	private void setOrAddReference(EObject source, EObject target, EcoreRelationship relationship) {
		if (target == null)
			return;
		System.out.println("Setting reference from a " + source.eClass().getName() + "." + relationship.name() + " to "
				+ target.eClass().getName());
		EReference eReference = relationship.wrapped();
		if (eReference.isMany())
			((List<EObject>) source.eGet(eReference)).add(target);
		else
			source.eSet(eReference, target);
	}
}
