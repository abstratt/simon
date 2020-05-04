package com.abstratt.simon.compiler.ecore;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcorePackage;

import com.abstratt.simon.compiler.Metamodel;
import com.abstratt.simon.compiler.ecore.EcoreMetamodel.EcoreType;

public class EcorePrimitiveValue extends EcoreValue  implements Metamodel.Primitive {

	public EcorePrimitiveValue(EDataType classifier) {
		super(classifier);
	}

	@Override
	public EObject newModelElement() {
		EDataType dataType = wrapped();
		EFactory factory = dataType.getEPackage().getEFactoryInstance();
		return (EObject) factory.createFromString(dataType, null);
	}

	@Override
	public Kind kind() {
		EDataType eClass = wrapped;
		if (EcorePackage.Literals.ESTRING == eClass)
			return Kind.String;
		if (EcorePackage.Literals.EINT == eClass || EcorePackage.Literals.EINTEGER_OBJECT == eClass)
			return Kind.Integer;
		throw new IllegalStateException();
	}

}
