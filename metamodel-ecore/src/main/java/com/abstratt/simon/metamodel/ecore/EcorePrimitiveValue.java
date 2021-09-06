package com.abstratt.simon.metamodel.ecore;

import com.abstratt.simon.metamodel.Metamodel;
import com.abstratt.simon.metamodel.Metamodel.PrimitiveKind;
import com.abstratt.simon.metamodel.ecore.java2ecore.MetaEcoreHelper;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcorePackage;

public class EcorePrimitiveValue extends EcoreValue<EClass> implements Metamodel.Primitive {

	public EcorePrimitiveValue(EClass classifier) {
		super(classifier);
	}

	@Override
	public boolean isInstantiable() {
		return false;
	}

	@Override
	public EObject newModelElement() {
		EClass dataType = wrapped();
		EFactory factory = dataType.getEPackage().getEFactoryInstance();
		EObject value = factory.create(dataType);
		return value;
	}

	public PrimitiveKind kind() {
		EDataType eClass = (EDataType) wrapped().getEStructuralFeature(MetaEcoreHelper.PRIMITIVE_VALUE_FEATURE)
				.getEType();
		if (EcorePackage.Literals.EBOOLEAN == eClass || EcorePackage.Literals.EBOOLEAN_OBJECT == eClass)
			return PrimitiveKind.Boolean;
		if (EcorePackage.Literals.EINT == eClass || EcorePackage.Literals.EINTEGER_OBJECT == eClass)
			return PrimitiveKind.Integer;
		if (EcorePackage.Literals.ELONG == eClass || EcorePackage.Literals.ELONG_OBJECT == eClass)
			return PrimitiveKind.Integer;
		if (EcorePackage.Literals.ESHORT == eClass || EcorePackage.Literals.ESHORT_OBJECT == eClass)
			return PrimitiveKind.Integer;
		if (EcorePackage.Literals.ECHAR == eClass || EcorePackage.Literals.ECHARACTER_OBJECT == eClass)
			return PrimitiveKind.Integer;
		if (EcorePackage.Literals.EDOUBLE == eClass || EcorePackage.Literals.EDOUBLE_OBJECT == eClass)
			return PrimitiveKind.Decimal;
		if (EcorePackage.Literals.EFLOAT == eClass || EcorePackage.Literals.EFLOAT_OBJECT == eClass)
			return PrimitiveKind.Decimal;
		if (EcorePackage.Literals.ESTRING == eClass)
			return PrimitiveKind.String;
		throw new IllegalStateException(eClass.getName());
	}

}
