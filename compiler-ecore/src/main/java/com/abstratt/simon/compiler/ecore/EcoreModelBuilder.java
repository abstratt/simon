package com.abstratt.simon.compiler.ecore;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;

import com.abstratt.simon.compiler.Configuration;
import com.abstratt.simon.compiler.Configuration.Instantiation;
import com.abstratt.simon.compiler.Configuration.Linking;
import com.abstratt.simon.compiler.Configuration.NameQuerying;
import com.abstratt.simon.compiler.Configuration.NameResolution;
import com.abstratt.simon.compiler.Configuration.NameSetting;
import com.abstratt.simon.compiler.Configuration.Operation;
import com.abstratt.simon.compiler.Configuration.Parenting;
import com.abstratt.simon.compiler.Configuration.ValueSetting;
import com.abstratt.simon.compiler.ecore.EcoreMetamodel.EcoreObjectType;
import com.abstratt.simon.compiler.ecore.EcoreMetamodel.EcoreRelationship;
import com.abstratt.simon.compiler.ecore.EcoreMetamodel.EcoreSlot;
import com.abstratt.simon.compiler.ecore.EcoreMetamodel.EcoreSlotted;
import com.abstratt.simon.metamodel.dsl.java2ecore.MetaEcoreHelper;

public class EcoreModelBuilder implements Configuration.Provider<EcoreObjectType, EcoreSlotted<?>, EObject> {

	private ThreadLocal<Resource> currentResource = new ThreadLocal<Resource>();
	
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
		return EcoreHelper::setName;
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

	public <E extends EObject> E createObject(boolean root, EcoreSlotted<?> resolvedType) {
		E newElement = (E) resolvedType.newModelElement();
		if (root) {
			Resource resource = currentResource.get();
			resource.getContents().add(newElement);
		}
		return newElement;
	}

	public EObject resolve(EObject scope, String... path) {
		EAttribute nameAttribute = EcoreHelper.findFeatureInHierarchy(scope, "name");
		EObject resolved = Traversal.search(nameAttribute, path).hop(scope);
//		if (resolved == null) {
//			// maybe a meta-reference? TODO-RC hacked together for hackathon, does this make sense at all for primitive types? probably not!
//			ENamedElement metaStart = scope.eClass().getEPackage();
//			resolved = Traversal.search(EcorePackage.Literals.ENAMED_ELEMENT__NAME, path).hop(metaStart);
//			if (resolved == null) {
//				EObject root = Traversal.root().hop(scope);
//				EcoreHelper.tree(root).forEach(System.out::println);
//			}
//		}
		EObject finalResolved = resolved;
		System.out.println("Resolved " + Arrays.asList(path) + " to " + Optional.ofNullable(resolved)
				.map(it -> getName(it) + " : " + finalResolved.eClass().getName()).orElse(null));
		return resolved;
	}

	public String getName(EObject named) {
		EStructuralFeature nameProperty = MetaEcoreHelper.getNameAttribute(named);
		if (nameProperty == null) {
			return null;
		}
		Object nameValue = named.eGet(nameProperty);
		if (nameValue == null || !(nameValue instanceof EObject)) {
			return (String) nameValue;
		}
		EObject nameAsValue = (EObject) nameValue;
		EStructuralFeature valueFeature = MetaEcoreHelper.getValueFeature(nameAsValue.eClass());
		return nameAsValue == null ? null : (String) nameAsValue.eGet(valueFeature);
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
		EAttribute eAttribute = slot.wrapped();
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
		System.out.println("Setting reference from a " + source.eClass().getName() + "." + relationship.name() + " to "
				+ target.eClass().getName());
		EReference eReference = relationship.wrapped();
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
