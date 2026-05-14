package com.abstratt.simon.compiler.source.simon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.ETypedElement;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;

import com.abstratt.simon.compiler.source.ecore.Java2EcoreMapper;
import com.abstratt.simon.metamodel.Metamodel.PrimitiveKind;
import com.abstratt.simon.metamodel.ecore.impl.EcoreHelper;
import com.abstratt.simon.metamodel.ecore.impl.MetaEcoreHelper;

/**
 * Builds EMF-based metamodels from {@code Simon.Package} EObject instances
 * produced by compiling {@code .simon} files against the bootstrap
 * {@code com.abstratt.simon.examples.Simon} metamodel.
 *
 * Structural sibling of {@link Java2EcoreMapper}: same wire-format output
 * ({@link EPackage} with Simon convention annotations), driven from
 * declarative source instead of Java reflection.
 */
public class Simon2EcoreMapper {

    private static final String SIMON_PACKAGE_TYPE = "Package";
    private static final String SIMON_OBJECT_TYPE = "ObjectType";
    private static final String SIMON_RECORD_TYPE = "RecordType";
    private static final String SIMON_ENUM_TYPE = "EnumType";
    private static final String SIMON_PRIMITIVE_TYPE = "PrimitiveType";
    private static final String SIMON_ATTRIBUTE = "Attribute";
    private static final String SIMON_CONTAINMENT = "Containment";
    private static final String SIMON_REFERENCE = "Reference";

    /** Maps each Simon classifier EObject to the EClassifier built from it. */
    private final Map<EObject, EClassifier> classifierRegistry = new HashMap<>();

    /** One primitive wrapper EClass per (consuming EPackage, PrimitiveKind). */
    private final Map<EPackage, Map<PrimitiveKind, EClass>> primitiveWrappersByPackage = new HashMap<>();

    /**
     * Translates the given parsed {@code Simon.Package} instances into Ecore
     * EPackages. All packages are placed in the same {@link Resource} so that
     * cross-package supertypes and feature types share identity.
     *
     * @return a map keyed by package name (in declaration order)
     */
    public Map<String, EPackage> map(Collection<EObject> packageInstances) {
        Resource resource = new ResourceImpl();
        Map<String, EPackage> packages = new LinkedHashMap<>();

        // Pass 1: shells. Create every EClass / EEnum and register them.
        for (EObject packageInstance : packageInstances) {
            EPackage ePackage = buildPackageShell(packageInstance);
            resource.getContents().add(ePackage);
            packages.put(ePackage.getName(), ePackage);
        }

        // Pass 2: enum literals (purely intra-classifier, no cross-refs).
        for (EObject packageInstance : packageInstances) {
            for (EObject enumType : children(packageInstance, "enumTypes")) {
                EEnum eEnum = (EEnum) classifierRegistry.get(enumType);
                int ordinal = 0;
                for (EObject literal : children(enumType, "literals")) {
                    EEnumLiteral eLiteral = EcoreFactory.eINSTANCE.createEEnumLiteral();
                    String name = stringValueOf(literal, "name");
                    eLiteral.setName(name);
                    eLiteral.setLiteral(name);
                    eLiteral.setValue(ordinal++);
                    eEnum.getELiterals().add(eLiteral);
                }
            }
        }

        // Pass 3: wire supertypes and features (cross-package references resolved
        // via the shared classifierRegistry).
        for (EObject packageInstance : packageInstances) {
            for (EObject objectType : children(packageInstance, "objectTypes")) {
                wireObjectType(objectType);
            }
            for (EObject recordType : children(packageInstance, "recordTypes")) {
                wireRecordType(recordType);
            }
        }

        // Pass 4: pair opposite references.
        pairOpposites(packageInstances);

        return packages;
    }

    private EPackage buildPackageShell(EObject packageInstance) {
        EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
        String name = stringValueOf(packageInstance, "name");
        ePackage.setName(name);
        ePackage.setNsURI(name);
        ePackage.setNsPrefix(name);

        for (EObject objectType : children(packageInstance, "objectTypes")) {
            EClass eClass = EcoreFactory.eINSTANCE.createEClass();
            eClass.setName(stringValueOf(objectType, "name"));
            eClass.setAbstract(booleanValueOf(objectType, "abstract"));
            eClass.setInterface(false);
            if (booleanValueOf(objectType, "root")) {
                MetaEcoreHelper.makeRootComposite(eClass);
            }
            ePackage.getEClassifiers().add(eClass);
            classifierRegistry.put(objectType, eClass);
        }
        for (EObject recordType : children(packageInstance, "recordTypes")) {
            EClass eClass = EcoreFactory.eINSTANCE.createEClass();
            eClass.setName(stringValueOf(recordType, "name"));
            MetaEcoreHelper.makeRecordType(eClass);
            ePackage.getEClassifiers().add(eClass);
            classifierRegistry.put(recordType, eClass);
        }
        for (EObject enumType : children(packageInstance, "enumTypes")) {
            EEnum eEnum = EcoreFactory.eINSTANCE.createEEnum();
            eEnum.setName(stringValueOf(enumType, "name"));
            ePackage.getEClassifiers().add(eEnum);
            classifierRegistry.put(enumType, eEnum);
        }
        // Primitive types from the source file declare a kind; no separate
        // classifier is added at the source's package — primitive use sites
        // materialize a wrapper EClass inside the consuming package on demand.
        // We still register the primitive instance so feature.type() lookups
        // can identify it.
        for (EObject primitiveType : children(packageInstance, "primitiveTypes")) {
            classifierRegistry.put(primitiveType, null);
        }
        return ePackage;
    }

    private void wireObjectType(EObject objectTypeInstance) {
        EClass eClass = (EClass) classifierRegistry.get(objectTypeInstance);
        for (EObject superType : references(objectTypeInstance, "superTypes")) {
            EClassifier resolved = classifierRegistry.get(superType);
            if (resolved instanceof EClass) {
                eClass.getESuperTypes().add((EClass) resolved);
            }
        }
        addFeatures(eClass, sortByName(children(objectTypeInstance, "attributes")), this::buildAttribute);
        addFeatures(eClass, sortByName(children(objectTypeInstance, "containments")), this::buildContainment);
        addFeatures(eClass, sortByName(children(objectTypeInstance, "references")), this::buildReference);
    }

    private void wireRecordType(EObject recordTypeInstance) {
        EClass eClass = (EClass) classifierRegistry.get(recordTypeInstance);
        addFeatures(eClass, sortByName(children(recordTypeInstance, "attributes")), this::buildAttribute);
    }

    private interface FeatureBuilder {
        EStructuralFeature build(EClass owner, EObject featureInstance);
    }

    private void addFeatures(EClass owner, List<EObject> featureInstances, FeatureBuilder builder) {
        for (EObject featureInstance : featureInstances) {
            EStructuralFeature feature = builder.build(owner, featureInstance);
            owner.getEStructuralFeatures().add(feature);
        }
    }

    private EAttribute buildAttribute(EClass owner, EObject attributeInstance) {
        EAttribute eAttribute = EcoreFactory.eINSTANCE.createEAttribute();
        eAttribute.setName(stripTrailingUnderscore(stringValueOf(attributeInstance, "name")));
        setMultiplicity(attributeInstance, eAttribute);
        String role = enumLiteralName(attributeInstance, "role");
        if ("Name".equals(role)) {
            MetaEcoreHelper.markAsName(eAttribute);
        } else if ("Documentation".equals(role)) {
            MetaEcoreHelper.markAsDocumentation(eAttribute);
        } else if ("Modifier".equals(role)) {
            MetaEcoreHelper.markAsModifier(eAttribute);
        }
        EClassifier eType = resolveAttributeType(owner, reference(attributeInstance, "type"));
        eAttribute.setEType(eType);
        return eAttribute;
    }

    /**
     * Reads an enum-valued attribute. Does NOT consult {@code eIsSet} — for
     * enum attributes EMF returns {@code eIsSet=false} whenever the value
     * equals the default (first literal), which is indistinguishable from
     * an explicit user assignment to that literal. Treat the raw value as
     * the source of truth.
     */
    private static String enumLiteralName(EObject target, String featureName) {
        EStructuralFeature feature = target.eClass().getEStructuralFeature(featureName);
        if (feature == null) {
            return null;
        }
        Object raw = target.eGet(feature);
        if (raw instanceof EEnumLiteral) {
            return ((EEnumLiteral) raw).getName();
        }
        if (raw instanceof Enum<?>) {
            return ((Enum<?>) raw).name();
        }
        return raw == null ? null : raw.toString();
    }

    private EReference buildContainment(EClass owner, EObject containmentInstance) {
        EReference eReference = baseReference(owner, containmentInstance);
        eReference.setContainment(true);
        return eReference;
    }

    private EReference buildReference(EClass owner, EObject referenceInstance) {
        return baseReference(owner, referenceInstance);
    }

    private EReference baseReference(EClass owner, EObject featureInstance) {
        EReference eReference = EcoreFactory.eINSTANCE.createEReference();
        eReference.setName(stripTrailingUnderscore(stringValueOf(featureInstance, "name")));
        setMultiplicity(featureInstance, eReference);
        EObject targetType = reference(featureInstance, "type");
        EClassifier resolved = classifierRegistry.get(targetType);
        if (resolved instanceof EClass) {
            eReference.setEType(resolved);
        }
        return eReference;
    }

    private void pairOpposites(Collection<EObject> packageInstances) {
        for (EObject packageInstance : packageInstances) {
            for (EObject objectType : children(packageInstance, "objectTypes")) {
                EClass owner = (EClass) classifierRegistry.get(objectType);
                for (EObject ref : sortByName(children(objectType, "references"))) {
                    String oppositeName = stringValueOf(ref, "opposite");
                    if (oppositeName == null || oppositeName.isEmpty()) {
                        continue;
                    }
                    EReference thisSide = (EReference) owner
                            .getEStructuralFeature(stripTrailingUnderscore(stringValueOf(ref, "name")));
                    if (thisSide == null || thisSide.getEReferenceType() == null) {
                        continue;
                    }
                    EReference opposite = thisSide.getEReferenceType().getEReferences().stream()
                            .filter(r -> oppositeName.equals(r.getName()))
                            .findFirst().orElse(null);
                    if (opposite != null) {
                        thisSide.setEOpposite(opposite);
                        opposite.setEOpposite(thisSide);
                    }
                }
            }
        }
    }

    private void setMultiplicity(EObject featureInstance, EStructuralFeature feature) {
        boolean multivalued = booleanValueOf(featureInstance, "multivalued");
        boolean explicitRequired = booleanValueOf(featureInstance, "required");
        boolean explicitOptional = booleanValueOf(featureInstance, "optional");
        boolean required;
        if (explicitRequired) {
            required = true;
        } else if (explicitOptional) {
            required = false;
        } else {
            required = !multivalued;
        }
        feature.setLowerBound(required ? 1 : 0);
        feature.setUpperBound(multivalued ? ETypedElement.UNBOUNDED_MULTIPLICITY : 1);
    }

    private EClassifier resolveAttributeType(EClass owner, EObject typeInstance) {
        if (typeInstance == null) {
            return null;
        }
        EClass typeKind = typeInstance.eClass();
        String kindName = typeKind.getName();
        if (SIMON_PRIMITIVE_TYPE.equals(kindName)) {
            return getOrCreatePrimitiveWrapper(owner.getEPackage(), typeInstance);
        }
        // Record types and enum types are used directly.
        return classifierRegistry.get(typeInstance);
    }

    private EClass getOrCreatePrimitiveWrapper(EPackage consumingPackage, EObject primitiveInstance) {
        PrimitiveKind kind = primitiveKindOf(primitiveInstance);
        Map<PrimitiveKind, EClass> wrappersForPackage = primitiveWrappersByPackage
                .computeIfAbsent(consumingPackage, p -> new HashMap<>());
        EClass cached = wrappersForPackage.get(kind);
        if (cached != null) {
            return cached;
        }
        EClass wrapper = EcoreFactory.eINSTANCE.createEClass();
        wrapper.setName(stringValueOf(primitiveInstance, "name"));
        EAttribute valueFeature = EcoreFactory.eINSTANCE.createEAttribute();
        valueFeature.setName(MetaEcoreHelper.PRIMITIVE_VALUE_FEATURE);
        valueFeature.setEType(MetaEcoreHelper.getPrimitiveEType(kind));
        wrapper.getEStructuralFeatures().add(valueFeature);
        MetaEcoreHelper.makePrimitiveType(wrapper, kind);
        consumingPackage.getEClassifiers().add(wrapper);
        wrappersForPackage.put(kind, wrapper);
        return wrapper;
    }

    private PrimitiveKind primitiveKindOf(EObject primitiveInstance) {
        String literal = enumLiteralName(primitiveInstance, "kind");
        if (literal == null) {
            throw new IllegalStateException("PrimitiveType '" + stringValueOf(primitiveInstance, "name")
                    + "' is missing a kind");
        }
        return PrimitiveKind.valueOf(literal);
    }

    private static List<EObject> children(EObject parent, String featureName) {
        EStructuralFeature feature = parent.eClass().getEStructuralFeature(featureName);
        if (feature == null) {
            return List.of();
        }
        Object value = parent.eGet(feature);
        if (value instanceof Collection<?>) {
            List<EObject> result = new ArrayList<>();
            for (Object item : (Collection<?>) value) {
                if (item instanceof EObject) {
                    result.add((EObject) item);
                }
            }
            return result;
        }
        return value instanceof EObject ? List.of((EObject) value) : List.of();
    }

    private static List<EObject> references(EObject parent, String featureName) {
        return children(parent, featureName);
    }

    private static EObject reference(EObject parent, String featureName) {
        EStructuralFeature feature = parent.eClass().getEStructuralFeature(featureName);
        if (feature == null) {
            return null;
        }
        Object value = parent.eGet(feature);
        return value instanceof EObject ? (EObject) value : null;
    }

    private static List<EObject> sortByName(List<EObject> items) {
        List<EObject> copy = new ArrayList<>(items);
        copy.sort(Comparator.comparing(o -> {
            String name = stringValueOf(o, "name");
            return name == null ? "" : stripTrailingUnderscore(name);
        }));
        return copy;
    }

    private static String stringValueOf(EObject target, String featureName) {
        EStructuralFeature feature = target.eClass().getEStructuralFeature(featureName);
        if (feature == null) {
            return null;
        }
        Object raw = target.eGet(feature);
        if (raw instanceof EObject) {
            Object unwrapped = EcoreHelper.unwrappedPrimitiveValue((EObject) raw);
            return unwrapped == null ? null : unwrapped.toString();
        }
        return raw == null ? null : raw.toString();
    }

    /**
     * Reads a boolean attribute. Defaults to false if unset or absent. Like
     * {@link #enumLiteralName}, deliberately ignores {@code eIsSet} — EMF
     * returns false there whenever the value matches the default (false),
     * which is what callers want here anyway. As a result we can't
     * distinguish "explicit false" from "unset"; the bootstrap metamodel is
     * designed around that limitation (e.g. exposing both {@code required}
     * and {@code optional} modifiers rather than one tri-state boolean).
     */
    private static boolean booleanValueOf(EObject target, String featureName) {
        EStructuralFeature feature = target.eClass().getEStructuralFeature(featureName);
        if (feature == null) {
            return false;
        }
        Object raw = target.eGet(feature);
        if (raw instanceof EObject) {
            raw = EcoreHelper.unwrappedPrimitiveValue((EObject) raw);
        }
        if (raw instanceof Boolean) {
            return (Boolean) raw;
        }
        return raw != null && Boolean.parseBoolean(raw.toString());
    }

    private static String stripTrailingUnderscore(String name) {
        return name == null ? null : name.replaceFirst("_$", "");
    }
}
