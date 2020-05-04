package com.abstratt.simon.compiler.ecore;

import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;

import com.abstratt.simon.compiler.Configuration;
import com.abstratt.simon.compiler.Configuration.Instantiation;
import com.abstratt.simon.compiler.Configuration.Linking;
import com.abstratt.simon.compiler.Configuration.Naming;
import com.abstratt.simon.compiler.Configuration.Parenting;
import com.abstratt.simon.compiler.Configuration.ValueSetting;
import com.abstratt.simon.compiler.ecore.EcoreMetamodel.EcoreObjectType;
import com.abstratt.simon.compiler.ecore.EcoreMetamodel.EcoreRelationship;
import com.abstratt.simon.compiler.ecore.EcoreMetamodel.EcoreSlot;

public class EcoreModelBuilder implements Configuration.Provider<EcoreObjectType, EObject> {

	@Override
	public Instantiation<EcoreObjectType> instantiation() {
		return this::createRootObject;
	}

	@Override
	public Linking<EObject, EcoreRelationship> linking() {
		return this::link;
	}
	
	@Override
	public Naming<EObject> naming() {
		return this::setName;
	}
	
	@Override
	public ValueSetting<EObject, EcoreSlot> valueSetting() {
		return this::setValue;
	}
	
	@Override
	public Parenting<EObject, EcoreRelationship> parenting() {
		return this::addChild;
	}
	
	public <E extends EObject> E createRootObject(EcoreObjectType resolvedType) {
		return (E) resolvedType.newModelElement();
	}
	
	public void setName(EObject named, String name) {
		//TODO-RC what to do about objects that do not support naming? Is it supported across the board?
		EStructuralFeature nameProperty = named.eClass().getEStructuralFeature("name");
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
		EReference eReference = relationship.wrapped();
		if (eReference.isMany()) 
			((List<EObject>) source.eGet(eReference)).add(target);
		else 
			source.eSet(eReference, target);
	}
}
