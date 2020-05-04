package com.abstratt.simon.compiler.ecore;

import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;

import com.abstratt.simon.compiler.Metamodel;

public class EcoreEnumValue extends EcoreValue implements Metamodel.Enumerated {

	public EcoreEnumValue(EDataType classifier) {
		super(classifier);
	}

	@Override
	public EObject newModelElement() {
		EDataType dataType = wrapped();
		EFactory factory = dataType.getEPackage().getEFactoryInstance();
		return (EObject) factory.createFromString(dataType, null);
	}

}
