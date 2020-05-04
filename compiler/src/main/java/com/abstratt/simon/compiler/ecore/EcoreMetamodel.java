package com.abstratt.simon.compiler.ecore;

import java.util.Collection;
import java.util.stream.Collectors;

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

import com.abstratt.simon.compiler.Metamodel;

public class EcoreMetamodel implements Metamodel {

	private EPackage ePackage;

	public EcoreMetamodel(EPackage ePackage) {
		this.ePackage = ePackage;
	}
	
//	@Override
//	public Collection<ObjectType> rootTypes() {
//		return ePackage.getEClassifiers().stream().filter(EcoreMetamodel::isRootEClass).map(ec -> (EClass) ec).map(EcoreObjectType::new).collect(Collectors.toList());
//	}
	
	private static boolean isRootEClass(EClassifier classifier) {
		return classifier instanceof EClass && ((EClass) classifier).eContainer() != null;
	}

	@Override
	public Type resolveType(String typeName) {
		EClassifier classifier = ePackage.getEClassifier(typeName);
		return classifier == null ? null : EcoreType.fromClassifier(classifier);
	}
	
	@Override
	public boolean isNamedObject(Type resolvedType) {
		return resolvedType instanceof EcoreSlotted<?>;
	}

	static class EcoreSlotted<T extends EClass> extends EcoreType<T> implements Slotted {
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
			return wrapped().getEPackage().getEFactoryInstance().create(wrapped());
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
				return new EcoreObjectType((EClass) classifier);
			}
			if (classifier instanceof EEnum) {
				return new EcoreEnumValue((EDataType) classifier);	
			}
			return new EcorePrimitiveValue((EDataType) classifier);
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

	}

}
