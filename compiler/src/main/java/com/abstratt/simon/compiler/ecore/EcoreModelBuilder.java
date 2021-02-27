package com.abstratt.simon.compiler.ecore;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreUtil;

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
import com.abstratt.simon.metamodel.dsl.java2ecore.MetaEcoreHelper;

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
		return MetaEcoreHelper::setName;
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
		if (resolved == null) {
			// maybe a meta-reference? TODO-RC hacked together for hackathon, does this make sense at all for primitive types? probably not!
			ENamedElement metaStart = scope.eClass().getEPackage();
			resolved = Traversal.search(EcorePackage.Literals.ENAMED_ELEMENT__NAME, path).hop(metaStart);
			if (resolved == null) {
				EObject root = Traversal.root().hop(scope);
				EcoreHelper.tree(root).forEach(System.out::println);
			}
		}
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
		return (String) named.eGet(nameProperty);
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
		target.eSet(eAttribute, MetaEcoreHelper.wrappedPrimitiveValue(eAttribute.eClass(), value));
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
