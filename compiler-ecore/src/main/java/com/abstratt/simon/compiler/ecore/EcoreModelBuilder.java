package com.abstratt.simon.compiler.ecore;

import java.util.List;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;

import com.abstratt.simon.compiler.ModelHandling;
import com.abstratt.simon.compiler.ModelHandling.Declaration;
import com.abstratt.simon.compiler.ModelHandling.Instantiation;
import com.abstratt.simon.compiler.ModelHandling.Linking;
import com.abstratt.simon.compiler.ModelHandling.NameQuerying;
import com.abstratt.simon.compiler.ModelHandling.NameResolution;
import com.abstratt.simon.compiler.ModelHandling.NameSetting;
import com.abstratt.simon.compiler.ModelHandling.Operation;
import com.abstratt.simon.compiler.ModelHandling.Parenting;
import com.abstratt.simon.compiler.ModelHandling.ValueSetting;
import com.abstratt.simon.genutils.Traversal;
import com.abstratt.simon.metamodel.ecore.java2ecore.EcoreHelper;
import com.abstratt.simon.metamodel.ecore.java2ecore.MetaEcoreHelper;
import com.abstratt.simon.metamodel.ecore.EcoreMetamodel.EcoreObjectType;
import com.abstratt.simon.metamodel.ecore.EcoreMetamodel.EcoreRelationship;
import com.abstratt.simon.metamodel.ecore.EcoreMetamodel.EcoreSlot;
import com.abstratt.simon.metamodel.ecore.EcoreMetamodel.EcoreSlotted;
import com.abstratt.simon.metamodel.ecore.EcorePrimitiveValue;

public class EcoreModelBuilder implements ModelHandling.Provider<EcoreObjectType, EcoreSlotted<?>, EObject> {

	private final ThreadLocal<Resource> currentResource = new ThreadLocal<Resource>();

	@Override
	public NameResolution<EObject> nameResolution() {
		return this::resolve;
	}

	@Override
	public Instantiation<EcoreSlotted<?>> instantiation() {
		return this::createObject;
	}
	
	@Override
	public Declaration<EcorePrimitiveValue> declaration() {
		return this::declarePrimitive;
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
	
	private <E extends EObject> E declarePrimitive(EcorePrimitiveValue primitiveType) {
		var newPrimitive = (E) primitiveType.newModelElement();
		setName(newPrimitive, primitiveType.name());
		addToResource(newPrimitive);
		return newPrimitive;
	}

	public <E extends EObject> E createObject(boolean root, EcoreSlotted<?> resolvedType) {
		var newElement = (E) resolvedType.newModelElement();
		if (root)
			addToResource(newElement);
		return newElement;
	}

	private <E extends EObject> void addToResource(E newElement) {
		var resource = currentResource.get();
		var contents = resource.getContents();
		contents.add(newElement);
		assert newElement.eResource() != null;
	}

	public EObject resolve(EObject scope, String... path) {
		EAttribute nameAttribute = EcoreHelper.findFeatureInHierarchy(scope, "name");
		Traversal<EObject> search = EObjectTraversalProvider.INSTANCE.search(nameAttribute, path);
		var resolved = search.hop(scope);
		return resolved;
	}

	public void setName(EObject unnamed, String newName) {
		EcoreHelper.setName(unnamed, newName);
	}
	
	public String getName(EObject named) {
		var nameProperty = MetaEcoreHelper.getNameAttribute(named);
		if (nameProperty == null) {
			return null;
		}
		var nameValue = named.eGet(nameProperty);
		if (!(nameValue instanceof EObject))
			return (String) nameValue;
		var nameAsValue = (EObject) nameValue;
		var valueFeature = MetaEcoreHelper.getValueFeature(nameAsValue.eClass());
		return (String) nameAsValue.eGet(valueFeature);
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
		var eAttribute = slot.wrapped();
		EObject valueAsEObject;
		if (MetaEcoreHelper.isPrimitive(eAttribute.getEType()))
			valueAsEObject = EcoreHelper.wrappedPrimitiveValue((EClass) eAttribute.getEType(), value);
		else
			valueAsEObject = (EObject) value;
		target.eSet(eAttribute, valueAsEObject);
	}

	private void setOrAddReference(EObject source, EObject target, EcoreRelationship relationship) {
		if (target == null)
			return;
		//System.out.println("Setting reference from a " + source.eClass().getName() + "." + relationship.name() + " to "
		//		+ target.eClass().getName());
		var eReference = relationship.wrapped();
		if (eReference.isMany())
			((List<EObject>) source.eGet(eReference)).add(target);
		else
			source.eSet(eReference, target);
	}
	
	@Override
	public <R> R runOperation(Operation<R> operation) {
		Resource resource = new ResourceImpl();
		currentResource.set(resource);
		try {
			return operation.run();
		} finally {
			currentResource.remove();
		}
	}
}
