package com.abstratt.simon.compiler.backend.ecore.impl;

import java.util.List;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;

import com.abstratt.simon.compiler.backend.Backend;
import com.abstratt.simon.compiler.backend.Declaration;
import com.abstratt.simon.compiler.backend.Instantiation;
import com.abstratt.simon.compiler.backend.Linking;
import com.abstratt.simon.compiler.backend.MetamodelException;
import com.abstratt.simon.compiler.backend.NameQuerying;
import com.abstratt.simon.compiler.backend.NameResolution;
import com.abstratt.simon.compiler.backend.NameSetting;
import com.abstratt.simon.compiler.backend.Operation;
import com.abstratt.simon.compiler.backend.Parenting;
import com.abstratt.simon.compiler.backend.ValueSetting;
import com.abstratt.simon.compiler.backend.ecore.EObjectTraversalProvider;
import com.abstratt.simon.genutils.Traversal;
import com.abstratt.simon.metamodel.ecore.EcoreMetamodel.EcoreObjectType;
import com.abstratt.simon.metamodel.ecore.EcoreMetamodel.EcoreRelationship;
import com.abstratt.simon.metamodel.ecore.EcoreMetamodel.EcoreSlot;
import com.abstratt.simon.metamodel.ecore.EcoreMetamodel.EcoreSlotted;
import com.abstratt.simon.metamodel.ecore.EcorePrimitiveValue;
import com.abstratt.simon.metamodel.ecore.impl.EcoreHelper;
import com.abstratt.simon.metamodel.ecore.impl.MetaEcoreHelper;

public class EcoreModelBuilder implements Backend<EcoreObjectType, EcoreSlotted<?>, EObject> {

    private final ThreadLocal<Resource> currentResource = new ThreadLocal<>();

    @Override
    public <R> R runOperation(Operation<R> operation) {
        currentResource.set(new ResourceImpl());
        try {
            return operation.run();
        } finally {
            currentResource.remove();
        }
    }

    @Override
    public NameResolution<EObject> nameResolution() {
        return this::resolve;
    }

    @Override
    public Instantiation<EcoreSlotted<EClass>> instantiation() {
        return this::createObject;
    }

    @Override
    public Declaration<EcorePrimitiveValue> declaration() {
        return this::declarePrimitive;
    }

    @Override
    public Linking<EObject, EcoreRelationship> linking() {
        return this::link;
    }

    @Override
    public NameSetting<EObject> nameSetting() {
        return this::setName;
    }

    @Override
    public NameQuerying<EObject> nameQuerying() {
        return this::getName;
    }

    @Override
    public ValueSetting<EObject, EcoreSlot> valueSetting() {
        return this::setValue;
    }

    @Override
    public Parenting<EObject, EcoreRelationship> parenting() {
        return this::addChild;
    }

    private <E extends EObject> E declarePrimitive(EcorePrimitiveValue primitiveType) {
        var newPrimitive = (E) primitiveType.newModelElement();
        setName(newPrimitive, primitiveType.name());
        addToResource(newPrimitive);
        return newPrimitive;
    }

    private <E extends EObject> E createObject(boolean root, EcoreSlotted<?> resolvedType) {
        var newElement = (E) resolvedType.newModelElement();
        if (root)
            addToResource(newElement);
        return newElement;
    }

    private <E extends EObject> void addToResource(E newElement) {
        var resource = currentResource.get();
        var contents = resource.getContents();
        contents.add(newElement);
        assert newElement.eResource() != null;
    }

    private EObject resolve(EObject scope, String... path) {
        EAttribute nameAttribute = EcoreHelper.findFeatureInHierarchy(scope, "name");
        Traversal<EObject> search = EObjectTraversalProvider.INSTANCE.search(nameAttribute, path);
        var resolved = search.hop(scope);
        return resolved;
    }

    private void setName(EObject unnamed, String newName) {
        EcoreHelper.setName(unnamed, newName);
    }

    private String getName(EObject named) {
        var nameProperty = MetaEcoreHelper.getNameAttribute(named);
        if (nameProperty == null) {
            return null;
        }
        var nameValue = named.eGet(nameProperty);
        if (!(nameValue instanceof EObject))
            return (String) nameValue;
        var nameAsValue = (EObject) nameValue;
        var valueFeature = MetaEcoreHelper.getValueFeature(nameAsValue.eClass());
        return (String) nameAsValue.eGet(valueFeature);
    }

    private void link(EcoreRelationship reference, EObject referrer, EObject referred) {
        try {
            setOrAddReference(referrer, referred, reference);
        } catch (ClassCastException e) {
            throw new MetamodelException(nameQuerying().getName(referred) + " cannot be referred to via "
                    + nameQuerying().getName(referrer) + "'s " + reference.name(), e);
        }
    }

    private void addChild(EcoreRelationship composition, EObject parent, EObject child) {
        try {
            setOrAddReference(parent, child, composition);
        } catch (ClassCastException e) {
            throw new MetamodelException(nameQuerying().getName(parent) + " cannot be added as a child to "
                    + nameQuerying().getName(parent) + "'s " + composition.name(), e);
        }
    }

    private void setParent(EcoreRelationship composition, EObject child, EObject parent) {
        child.eSet(composition.wrapped(), parent);
    }

    private void setValue(EcoreSlot slot, EObject target, Object value) {
        var eAttribute = slot.wrapped();
        EObject valueAsEObject;
        if (MetaEcoreHelper.isPrimitive(eAttribute.getEType()))
            valueAsEObject = EcoreHelper.wrappedPrimitiveValue((EClass) eAttribute.getEType(), value);
        else
            valueAsEObject = (EObject) value;
        target.eSet(eAttribute, valueAsEObject);
    }

    private void setOrAddReference(EObject source, EObject target, EcoreRelationship relationship) {
        // System.out.println("Setting reference from a " + source.eClass().getName() +
        // "." + relationship.name() + " to "
        // + target.eClass().getName());
        var eReference = relationship.wrapped();
        if (eReference.isMany())
            ((List<EObject>) source.eGet(eReference)).add(target);
        else
            source.eSet(eReference, target);
    }
}
