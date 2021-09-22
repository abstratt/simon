package com.abstratt.simon.compiler.backend.ecore;

import org.eclipse.emf.ecore.EObject;

import com.abstratt.simon.compiler.backend.Backend;
import com.abstratt.simon.compiler.backend.ecore.impl.EcoreModelBuilder;
import com.abstratt.simon.metamodel.ecore.EcoreMetamodel.EcoreObjectType;
import com.abstratt.simon.metamodel.ecore.EcoreMetamodel.EcoreSlotted;

public class EMFModelBackendFactory implements Backend.Factory {

    @Override
    public Backend<EcoreObjectType, EcoreSlotted<?>, EObject> create() {
        return new EcoreModelBuilder();
    }

}
