package com.abstratt.simon.metamodel.ecore;

import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;

import com.abstratt.simon.metamodel.Metamodel;

public class EcoreEnumValue extends EcoreValue<EEnum> implements Metamodel.Enumerated {

    public EcoreEnumValue(EEnum classifier) {
        super(classifier);
    }

    @Override
    public boolean isInstantiable() {
        return false;
    }

    @Override
    public EObject newModelElement() {
        EEnum dataType = wrapped();
        EFactory factory = dataType.getEPackage().getEFactoryInstance();
        return (EObject) factory.createFromString(dataType, null);
    }

    @Override
    public Object valueForName(String valueName) {
        return wrapped.getEEnumLiteral(valueName);
    }

}
