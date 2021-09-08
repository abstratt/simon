package com.abstratt.simon.metamodel.ecore;

import java.util.Collection;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.ETypedElement;

import com.abstratt.simon.metamodel.Metamodel;
import com.abstratt.simon.metamodel.ecore.impl.MetaEcoreHelper;

public interface EcoreMetamodel extends Metamodel {

	static boolean isRootEClass(EClassifier classifier) {
		return classifier instanceof EClass && MetaEcoreHelper.isRootComposite((EClass) classifier);
	}

	abstract class EcoreSlotted<T extends EClass> extends EcoreType<T> implements Slotted {
		public EcoreSlotted(T wrapped) {
			super(wrapped);
		}

		@Override
		public boolean isInstantiable() {
			return !wrapped.isAbstract();
		}

		@Override
		public Collection<Slot> slots() {
			EList<EAttribute> allAttributes = wrapped().getEAllAttributes();
			return allAttributes.stream().map(EcoreSlot::new).collect(Collectors.toList());
		}

		@Override
		public EObject newModelElement() {
			T eClass = wrapped();
			EObject newObject = instantiateEClass(eClass);
			return newObject;
		}

		protected EObject instantiateEClass(EClass eClass) {
			return eClass.getEPackage().getEFactoryInstance().create(eClass);
		}
	}

	abstract class EcoreNamed<N extends ENamedElement> implements Metamodel.Named {
		protected final N wrapped;

		public EcoreNamed(N wrapped) {
			this.wrapped = wrapped;
		}

		@Override
		public String name() {
			return wrapped.getName();
		}

		public N wrapped() {
			return wrapped;
		}

	}

	abstract class EcoreType<EC extends EClassifier> extends EcoreNamed<EC> implements Metamodel.Type {

		public EcoreType(EC wrapped) {
			super(wrapped);
		}

		public static EcoreType<?> fromClassifier(EClassifier classifier) {
			if (classifier instanceof EClass) {
				boolean isRecord = MetaEcoreHelper.isRecord(classifier);
				if (isRecord)
					return new EcoreRecordType((EClass) classifier);
				boolean isPrimitive = MetaEcoreHelper.isPrimitive(classifier);
				if (isPrimitive)
					return new EcorePrimitiveValue((EClass) classifier);
				return new EcoreObjectType((EClass) classifier);
			}
			if (classifier instanceof EEnum) {
				return new EcoreEnumValue((EEnum) classifier);
			}
			return new EcorePrimitiveValue((EClass) classifier);
		}

		@Override
		public boolean isRoot() {
			return isRootEClass(wrapped());
		}

		public abstract EObject newModelElement();

	}

	class EcoreFeature<TE extends ETypedElement, C extends EClassifier, T extends Type> extends EcoreNamed<TE>
			implements Metamodel.Feature<T> {

		public EcoreFeature(TE wrapped) {
			super(wrapped);
		}

		@Override
		public boolean required() {
			return wrapped().isRequired();
		}

		@Override
		public boolean multivalued() {
			return wrapped().isMany();
		}

		@Override
		public T type() {
			return (T) EcoreType.fromClassifier(wrapped().getEType());
		}

		public static <TE extends ETypedElement, T extends Type> Feature<T> create(TE feature) {
			if (feature instanceof EAttribute)
				return (Feature<T>) new EcoreSlot((EAttribute) feature);
			if (feature instanceof EReference)
				return (Feature<T>) new EcoreRelationship((EReference) feature);
			assert false : feature;
			return null;
		}

	}

	class EcoreRelationship extends EcoreFeature<EReference, EClass, ObjectType>
			implements Composition, Reference {

		public EcoreRelationship(EReference wrapped) {
			super(wrapped);
		}
	}

	class EcoreSlot extends EcoreFeature<EAttribute, EClassifier, BasicType> implements Slot {

		public EcoreSlot(EAttribute wrapped) {
			super(wrapped);
		}
	}

	class EcoreObjectType extends EcoreSlotted<EClass> implements Metamodel.ObjectType {

		public EcoreObjectType(EClass wrapped) {
			super(wrapped);
		}

		@Override
		public Collection<Composition> compositions() {
			return wrapped().getEAllContainments().stream().map(EcoreRelationship::new).collect(Collectors.toList());
		}

		@Override
		public Collection<Reference> references() {
			return wrapped().getEAllReferences().stream().filter(ref -> !ref.isContainment() && !ref.isContainer())
					.map(EcoreRelationship::new).collect(Collectors.toList());
		}

		@Override
		public Collection<Feature> features() {
			return wrapped().getEAllStructuralFeatures().stream().map(EcoreFeature::create)
					.collect(Collectors.toList());
		}

		@Override
		public EObject newModelElement() {
			EObject newModelElement = super.newModelElement();
			return newModelElement;
		}

	}

	class EcoreRecordType extends EcoreSlotted<EClass> implements Metamodel.RecordType {

		public EcoreRecordType(EClass wrapped) {
			super(wrapped);
		}
	}
}
