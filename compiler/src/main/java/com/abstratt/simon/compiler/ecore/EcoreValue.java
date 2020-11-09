package com.abstratt.simon.compiler.ecore;

import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;

import com.abstratt.simon.compiler.Metamodel;
import com.abstratt.simon.compiler.ecore.EcoreMetamodel.EcoreType;

public abstract class EcoreValue<DT extends EDataType> extends EcoreType<DT> implements Metamodel.BasicType {

	public EcoreValue(DT classifier) {
		super(classifier);
	}

	@Override
	public EObject newModelElement() {
		EDataType dataType = wrapped();
		EFactory factory = dataType.getEPackage().getEFactoryInstance();
		return (EObject) factory.createFromString(dataType, null);
	}

}
