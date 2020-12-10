package com.abstratt.simon.compiler.ecore;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.ETypedElement;
import org.eclipse.emf.ecore.util.EcoreUtil;

import com.abstratt.simon.metamodel.Metamodel;
import com.abstratt.simon.metamodel.dsl.java2ecore.MetaEcoreHelper;

public interface EcoreMetamodel extends Metamodel {

//	@Override
//	public Collection<ObjectType> rootTypes() {
//		return ePackage.getEClassifiers().stream().filter(EcoreMetamodel::isRootEClass).map(ec -> (EClass) ec).map(EcoreObjectType::new).collect(Collectors.toList());
//	}
	
	public static boolean isRootEClass(EClassifier classifier) {
		return classifier instanceof EClass && ((EClass) classifier).eContainer() != null;
	}

	public abstract static class EcoreSlotted<T extends EClass> extends EcoreType<T> implements Slotted {
		public EcoreSlotted(T wrapped) {
			super(wrapped);
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

	static abstract class EcoreNamed<N extends ENamedElement> implements Metamodel.Named {
		protected N wrapped;

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

	static abstract class EcoreType<EC extends EClassifier> extends EcoreNamed<EC> implements Metamodel.Type {

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

	static class EcoreTyped<TE extends ETypedElement, C extends EClassifier, T extends Type> extends EcoreNamed<TE>
			implements Metamodel.Typed<T> {

		public EcoreTyped(TE wrapped) {
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

	}

	static class EcoreRelationship extends EcoreTyped<EReference, EClass, ObjectType>
			implements Composition, Reference {

		public EcoreRelationship(EReference wrapped) {
			super(wrapped);
		}
	}

	static class EcoreSlot extends EcoreTyped<EAttribute, EClassifier, BasicType> implements Slot {

		public EcoreSlot(EAttribute wrapped) {
			super(wrapped);
		}
	}

	static class EcoreObjectType extends EcoreSlotted<EClass> implements Metamodel.ObjectType {

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
		public EObject newModelElement() {
			EObject newModelElement = super.newModelElement();
			if (MetaEcoreHelper.isRootComposite(wrapped())) {
				//TODO-RC HACK! Finds all primitives in the metamodel and instatiate them here
				//btw, these types are floating around
				wrapped.getEPackage().getEClassifiers().stream() //
					.filter(MetaEcoreHelper::isPrimitive)//
					.map(it -> (EClass) it)//
					.map(this::instantiateEClass)//
					.forEach(e ->
						MetaEcoreHelper.setName(e, e.eClass().getName())
					);
			}
			return newModelElement;
		}

	}
	
	static class EcoreRecordType extends EcoreSlotted<EClass> implements Metamodel.RecordType {

		public EcoreRecordType(EClass wrapped) {
			super(wrapped);
		}
	}
}
