package com.abstratt.simon.metamodel.dsl.java2ecore;

import java.util.Objects;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.ETypedElement;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreUtil;

import com.abstratt.simon.metamodel.Metamodel;
import com.abstratt.simon.metamodel.Metamodel.PrimitiveKind;
import com.abstratt.simon.metamodel.dsl.Meta;
import com.abstratt.simon.metamodel.dsl.Meta.PrimitiveType;

public interface MetaEcoreHelper {
	static final String ROOT_COMPOSITE_VALUE = "rootComposite";
	static final String PRIMITIVE_TYPE_KIND = "kind";
	static final String PRIMITIVE_TYPE = Meta.PrimitiveType.class.getSimpleName();
	static final String RECORD_TYPE = Meta.RecordType.class.getSimpleName();
	static final String SIMON_ANNOTATION = "simon.annotations";
	static final String PRIMITIVE_VALUE_FEATURE = "__value__";

	static void makeRootComposite(EClass eClass) {
		EcoreUtil.setAnnotation(eClass, MetaEcoreHelper.SIMON_ANNOTATION, MetaEcoreHelper.ROOT_COMPOSITE_VALUE, Boolean.toString(true));
	}

	static void makeRecordType(EClass eClass) {
		EcoreUtil.setAnnotation(eClass, MetaEcoreHelper.SIMON_ANNOTATION, MetaEcoreHelper.RECORD_TYPE, Boolean.toString(true));		
	}

	static void makePrimitiveType(EClass eClass, Metamodel.PrimitiveKind kind) {
		setAnnotation(eClass, MetaEcoreHelper.PRIMITIVE_TYPE, Boolean.toString(true));
		setAnnotation(eClass, MetaEcoreHelper.PRIMITIVE_TYPE_KIND, kind.name());
	}

	static void setAnnotation(EClass eClass, String tag, String value) {
		EcoreUtil.setAnnotation(eClass, MetaEcoreHelper.SIMON_ANNOTATION, tag, value);
	}
	
	static boolean isRootComposite(EClass eClass) {
		String tag = MetaEcoreHelper.ROOT_COMPOSITE_VALUE;
		return Boolean.TRUE.toString().equals(getAnnotation(eClass, tag));
	}

	static String getAnnotation(EModelElement eClass, String tag) {
		return EcoreUtil.getAnnotation(eClass, MetaEcoreHelper.SIMON_ANNOTATION, tag);
	}
	
	static boolean isPrimitive(EClassifier eClass) {
		String tag = MetaEcoreHelper.PRIMITIVE_TYPE;
		return Boolean.TRUE.toString().equals(getAnnotation(eClass, tag));
	}
	
	static Metamodel.PrimitiveKind getPrimitiveKind(EClassifier eClass) {
		if (eClass == EcorePackage.eINSTANCE.getEString())
			return PrimitiveKind.String;
		if (eClass == EcorePackage.eINSTANCE.getEInt() || eClass == EcorePackage.eINSTANCE.getEIntegerObject()) 
			return PrimitiveKind.Integer;
		if (eClass == EcorePackage.eINSTANCE.getEBoolean() || eClass == EcorePackage.eINSTANCE.getEBooleanObject()) 
			return PrimitiveKind.Boolean;
		if (eClass == EcorePackage.eINSTANCE.getEBigDecimal()) 
			return PrimitiveKind.Decimal;
		
		throw new IllegalArgumentException(eClass.getName());
	}
	
	static boolean isRecord(EClassifier eClass) {
		String tag = MetaEcoreHelper.RECORD_TYPE;
		return Boolean.TRUE.toString().equals(getAnnotation(eClass, tag));
	}
	
	static boolean isPackage(Class<?> clazz) {
		return clazz.isAnnotationPresent(Meta.Package.class);
	}

	static boolean isEnum(Class<?> clazz) {
		return clazz.isEnum();
	}

	static boolean isRecord(Class<?> clazz) {
		return clazz.isAnnotationPresent(Meta.RecordType.class);
	}

	static boolean isPrimitive(Class<?> clazz) {
		return clazz.isAnnotationPresent(Meta.PrimitiveType.class);
	}
	
	static PrimitiveKind getPrimitiveKind(Class<?> clazz) {
		assert isPrimitive(clazz) : clazz;
		return clazz.getAnnotation(Meta.PrimitiveType.class).value();
	}
	
	static EDataType getPrimitiveEType(PrimitiveKind primitiveKind) {
		switch (primitiveKind) {
		case Boolean: return EcorePackage.Literals.EBOOLEAN_OBJECT;
		case Integer : return EcorePackage.Literals.EINTEGER_OBJECT;
		case Decimal : return EcorePackage.Literals.EBIG_DECIMAL;
		case String: 
		case Other:
			return EcorePackage.Literals.ESTRING;
		}
		throw new IllegalArgumentException(primitiveKind.name());
	}

	static EDataType getPrimitiveEType(Class<?> clazz) {
		if (isPrimitive(clazz))
			return getPrimitiveEType(getPrimitiveKind(clazz));
		Class<?> wrapperClass = EcoreUtil.wrapperClassFor(clazz);
		if (wrapperClass == Boolean.class) 
			return EcorePackage.Literals.EBOOLEAN_OBJECT;
		if (wrapperClass == Integer.class) 
			return EcorePackage.Literals.EINTEGER_OBJECT;
		if (wrapperClass == String.class) 
			return EcorePackage.Literals.ESTRING;
		
		throw new IllegalArgumentException(clazz.getName());
	}



	static boolean isRootComposite(Class<?> clazz) {
		return clazz.isAnnotationPresent(Meta.Composite.class) && clazz.getAnnotation(Meta.Composite.class).root();
	}
	
	static boolean isComposite(Class<?> clazz) {
		return clazz.isAnnotationPresent(Meta.Composite.class);
	}

	static void setName(EObject named, String name) {
		System.out.println("Setting name of a " + named.eClass().getName() + " to " + name);
		// TODO-RC what to do about objects that do not support naming? Is it supported
		// across the board?
		EStructuralFeature nameProperty = getNameAttribute(named);
		if (nameProperty == null) {
			throw new IllegalArgumentException("No 'name' feature in '" + named.eClass().getName());
		}
		named.eSet(nameProperty, wrappedPrimitiveValue((EClass) nameProperty.getEType(), name));
	}
	
	static EStructuralFeature getValueFeature(EClass labelType) {
		EStructuralFeature valueFeature = labelType.getEStructuralFeature(MetaEcoreHelper.PRIMITIVE_VALUE_FEATURE);
		return valueFeature;
	}
	
	static EDataType getValueType(ETypedElement name) {
		return (EDataType) MetaEcoreHelper.getValueFeature((EClass) name.getEType()).getEType();
	}

	static EObject wrappedPrimitiveValue(EClass eClass, Object value) {
		EObject wrapper = EcoreUtil.create(eClass);
		wrapper.eSet(getValueFeature(eClass), value);
		return wrapper;
	}

	static EAttribute getNameAttribute(EObject named) {
		EClass eClass = named.eClass();
		return getNameAttribute(eClass);
	}

	static EAttribute getNameAttribute(EClass eClass) {
		return (EAttribute) eClass.getEStructuralFeature("name");
	}


}
