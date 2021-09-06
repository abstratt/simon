package com.abstratt.simon.metamodel.ecore;

import com.abstratt.simon.metamodel.Metamodel;
import com.abstratt.simon.metamodel.ecore.EcoreMetamodel.EcoreType;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;

public abstract class EcoreValue<DT extends EClassifier> extends EcoreType<DT> implements Metamodel.BasicType {

	public EcoreValue(DT classifier) {
		super(classifier);
	}

	@Override
	public EObject newModelElement() {
		EDataType dataType = (EDataType) wrapped();
		EFactory factory = dataType.getEPackage().getEFactoryInstance();
		return (EObject) factory.createFromString(dataType, null);
	}

}
