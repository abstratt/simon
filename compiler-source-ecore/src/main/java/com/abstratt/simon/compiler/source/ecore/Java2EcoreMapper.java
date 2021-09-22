package com.abstratt.simon.compiler.source.ecore;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.ETypedElement;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.util.EcoreUtil;

import com.abstratt.simon.metamodel.dsl.Meta;
import com.abstratt.simon.metamodel.dsl.Meta.Required;
import com.abstratt.simon.metamodel.ecore.impl.MappingSession;
import com.abstratt.simon.metamodel.ecore.impl.MetaEcoreHelper;

/**
 * Builds an EMF-based metamodel from Java classes that have been marked with
 * Simon DSL annotations as defined in {@link Meta}.
 */
public class Java2EcoreMapper {
    /**
     * Maps the given Java class that utilizes Simon's annotation-based DSL to an
     * EMF-based metamodel.
     */
    public <E extends EObject> E map(Class<?> clazz) {
        // System.out.println("*** " + Arrays.stream(new
        // Throwable().fillInStackTrace().getStackTrace()).skip(1)
        // .findFirst().get().toString());

        MappingSession mappingSession = new MappingSession();
        Object[] result = { null };
        buildIfNeeded(clazz, mappingSession, debug("building if needed", r -> result[0] = r));
        // System.out.println("**** Resolving pending requests");
        mappingSession.processPendingRequests();
        // System.out.println("**** Done");

        return (E) result[0];
    }

    /**
     * Builds the model element corresponding to the given Java type, if needed.
     * 
     * @param <E>            the concrete model element
     * @param clazz          the Java type
     * @param mappingSession the resolution mappingSession
     * @param consumer       a consumer for the built or resolved element
     */
    private <E extends ENamedElement> void buildIfNeeded(Class<?> clazz, MappingSession mappingSession,
            Consumer<E> consumer) {
        System.out.println("Building if needed: " + clazz.getName());

        if (MetaEcoreHelper.isPrimitive(clazz) || MetaEcoreHelper.isPrimitiveJavaClass(clazz)) {
            buildIfNeeded(mappingSession.currentPackage(), clazz, mappingSession, consumer);
            return;
        }
        if (MetaEcoreHelper.isPackage(clazz)) {
            mappingSession.mapRoot(null, clazz, this::buildPackage, (Consumer<EPackage>) consumer);
            return;
        }

        Class<?> packageClass = getPackageClass(clazz);
        if (!MetaEcoreHelper.isPackage(packageClass))
            // TODO-RC not a metamodel class, what to do?
            throw new IllegalArgumentException("Not a metamodel class:" + clazz.getName());

        // may need to build the package first
        buildIfNeeded(packageClass, mappingSession, (EPackage resolvedEPackage) -> {
            System.out.println("Discovered parent package: " + resolvedEPackage.getName());
            buildIfNeeded(resolvedEPackage, clazz, mappingSession, consumer);
        });

    }

    private <E extends ENamedElement> void buildIfNeeded(EPackage parentPackage, Class<?> clazz,
            MappingSession mappingSession, Consumer<E> consumer) {
        System.out.println("Building if needed: " + clazz.getName() + " under " + parentPackage.getName());
        assert parentPackage != null : clazz.getName();
        mappingSession.runWithPackage(parentPackage, "findOrBuild(" + clazz.getName() + ")", ctx -> {
            if (MetaEcoreHelper.isPrimitive(clazz) || MetaEcoreHelper.isPrimitiveJavaClass(clazz)) {
                mappingSession.mapUnder(parentPackage, clazz, clazz, this::buildPrimitiveType,
                        (Consumer<EClass>) consumer);
            } else if (MetaEcoreHelper.isEnum(clazz)) {
                mappingSession.mapChild(clazz, clazz, this::buildEnumType, (Consumer<EEnum>) consumer);
            } else if (MetaEcoreHelper.isRecord(clazz)) {
                mappingSession.mapChild(clazz, clazz, this::buildRecordType, (Consumer<EClass>) consumer);
            } else {
                mappingSession.mapChild(clazz, clazz, this::buildObjectType, (Consumer<EClass>) consumer);
            }

        });
    }

    private static <J extends AnnotatedElement, E extends ENamedElement> String toString(J javaElement, E result) {
        return javaElement + " => " + toString(result);
    }

    private static <E extends ENamedElement> String toString(E result) {
        return result.getName() + " (" + result.eClass().getName() + ")";
    }

    private EPackage buildPackage(MappingSession mappingSession, Class<?> packageClass) {
        EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
        ePackage.setNsURI(packageClass.getSimpleName());
        ePackage.setNsPrefix(packageClass.getSimpleName());
        ePackage.setName(packageClass.getSimpleName());

        var packageAnnotation = packageClass.getAnnotation(Meta.Package.class);
        var builtIns = packageAnnotation.builtIns();
        storeBuiltIns(packageClass, ePackage, builtIns);

        mappingSession.runWithPackage(ePackage, "Building classes in package " + packageClass.getName(), ctx -> {
            for (Class<?> nested : packageClass.getClasses()) {
                this.<EClassifier>buildIfNeeded(nested, mappingSession,
                        debug("Adding to " + ePackage.getName(), ePackage.getEClassifiers()::add));
            }
        });
        return ePackage;
    }

    private void storeBuiltIns(Class<?> packageClass, EPackage ePackage, String[] builtIns) {
        var builtInsEAnnotation = EcoreFactory.eINSTANCE.createEAnnotation();
        builtInsEAnnotation.setSource("simon/builtIns");
        builtInsEAnnotation.setEModelElement(ePackage);
        var builtInsEAnnDetails = builtInsEAnnotation.getDetails();
        for (String builtIn : builtIns) {
            URL resource = packageClass.getResource(builtIn + ".simon");
            if (resource == null) {
                throw new RuntimeException("Could not find built-in '" + builtIn + "' for " + packageClass);
            }
            try {
                builtInsEAnnDetails.put(builtIn, IOUtils.toString(resource, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException("Could not read built-in '" + builtIn + "' for " + packageClass, e);
            }
        }
        ePackage.getEAnnotations().add(builtInsEAnnotation);
    }

//	private EDataType buildPrimitiveType(Context mappingSession, Class<?> clazz) {
//		String className = clazz.getSimpleName();
//		System.out.println("Building primitive type " + className);
//		EDataType eDataType = EcoreFactory.eINSTANCE.createEDataType();
//		eDataType.setName(className);
//		eDataType.setInstanceTypeName(className);
//		eDataType.setInstanceClass(clazz);
//		return eDataType;
//	}

    private EClass buildPrimitiveType(MappingSession mappingSession, Class<?> clazz) {
        // a primitive type is a class without any features
        String className = clazz.getSimpleName();
        // System.out.println("Building primitive type " + className);
        EClass eClass = EcoreFactory.eINSTANCE.createEClass();
        eClass.setName(className);
        EAttribute eAttribute = EcoreFactory.eINSTANCE.createEAttribute();
        eAttribute.setName(MetaEcoreHelper.PRIMITIVE_VALUE_FEATURE);
        EDataType primitiveType = MetaEcoreHelper.getPrimitiveEType(clazz);
        eAttribute.setEType(primitiveType);
        eClass.getEStructuralFeatures().add(eAttribute);
        MetaEcoreHelper.makePrimitiveType(eClass, MetaEcoreHelper.getPrimitiveKind(primitiveType));
        addSupertypes(mappingSession, clazz, eClass);
        return eClass;
    }

    private EEnum buildEnumType(MappingSession mappingSession, Class<?> clazz) {
        var className = clazz.getSimpleName();
        // System.out.println("Building enum type " + className);
        var eEnum = EcoreFactory.eINSTANCE.createEEnum();
        eEnum.setName(className);
        var enumConstants = clazz.getEnumConstants();
        for (Object it : enumConstants) {
            var asEnumConst = (Enum<?>) it;
            var literal = EcoreFactory.eINSTANCE.createEEnumLiteral();
            var valueAsStr = asEnumConst.name();
            literal.setName(valueAsStr);
            literal.setLiteral(valueAsStr);
            literal.setValue(asEnumConst.ordinal());
            eEnum.getELiterals().add(literal);
        }
        return eEnum;
    }

    private EClass buildRecordType(MappingSession mappingSession, Class<?> clazz) {
        String className = clazz.getSimpleName();
        // System.out.println("Building record type " + className);
        EClass eClass = EcoreFactory.eINSTANCE.createEClass();
        eClass.setName(className);
        MetaEcoreHelper.makeRecordType(eClass);
        resolvePackageAndAddClassifier(mappingSession, clazz, eClass);
        addSupertypes(mappingSession, clazz, eClass);
        addAttributes(mappingSession, clazz, eClass);
        return eClass;
    }

    private EClass buildObjectType(MappingSession mappingSession, Class<?> clazz) {
        String className = clazz.getSimpleName();
        System.out.println("Building object type " + className);
        boolean isInterface = Modifier.isInterface(clazz.getModifiers());
        boolean isAbstractClass = Modifier.isAbstract(clazz.getModifiers());
        boolean isRootComposite = MetaEcoreHelper.isRootComposite(clazz);
        EClass eClass = EcoreFactory.eINSTANCE.createEClass();
        if (isRootComposite) {
            EcoreUtil.setAnnotation(eClass, MetaEcoreHelper.SIMON_ANNOTATION, MetaEcoreHelper.ROOT_COMPOSITE_VALUE,
                    Boolean.toString(true));
        }
        addSupertypes(mappingSession, clazz, eClass);

        eClass.setName(className);
        eClass.setAbstract(isAbstractClass);
        eClass.setInterface(isInterface);
        addContainments(mappingSession, clazz, eClass);
        addReferences(mappingSession, clazz, eClass);
        addAttributes(mappingSession, clazz, eClass);
        resolvePackageAndAddClassifier(mappingSession, clazz, eClass);
        return eClass;
    }

    private void addSupertypes(MappingSession mappingSession, Class<?> clazz, EClass eClass) {
        Class<?> superJavaClass = clazz.getSuperclass();
        if (superJavaClass != null) {
            collectSuperType(mappingSession, clazz, superJavaClass,
                    debug("Setting super type on " + eClass.getName(), eClass.getESuperTypes()::add));
        }
        for (Class<?> i : clazz.getInterfaces()) {
            collectSuperType(mappingSession, clazz, i,
                    debug("Setting interface as super type on " + eClass.getName(), eClass.getESuperTypes()::add));
        }
    }

    private void resolvePackageAndAddClassifier(MappingSession mappingSession, Class<?> clazz, EClass eClass) {
        buildIfNeeded(getPackageClass(clazz), mappingSession,
                (EPackage resolvedPackage) -> addClassifierToPackage(eClass, resolvedPackage));
    }

    public boolean addClassifierToPackage(EClass eClass, EPackage resolvedPackage) {
        return resolvedPackage.getEClassifiers().add(eClass);
    }

    private Class<?> getPackageClass(Class<?> containedClazz) {
        assert containedClazz.isMemberClass() : containedClazz;
        Class<?> packageClass = containedClazz.getEnclosingClass();
        while (packageClass.getEnclosingClass() != null)
            packageClass = packageClass.getEnclosingClass();
        return packageClass;
    }

    private abstract static class Request {
        protected final Class<?> classToResolve;

        public Request(Class<?> classToResolve) {
            this.classToResolve = classToResolve;
        }
    }

    private static class SuperClassRequest extends Request {
        private final Class<?> baseJavaClass;

        public SuperClassRequest(Class<?> baseJavaClass, Class<?> superJavaClass) {
            super(superJavaClass);
            this.baseJavaClass = baseJavaClass;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((baseJavaClass == null) ? 0 : baseJavaClass.hashCode());
            result = prime * result + ((classToResolve == null) ? 0 : classToResolve.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            SuperClassRequest other = (SuperClassRequest) obj;
            if (baseJavaClass == null) {
                if (other.baseJavaClass != null)
                    return false;
            } else if (!baseJavaClass.equals(other.baseJavaClass))
                return false;
            if (classToResolve == null) {
                return other.classToResolve == null;
            } else
                return classToResolve.equals(other.classToResolve);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + " [classToResolve=" + classToResolve + "]";
        }
    }

    private void collectSuperType(MappingSession mappingSession, Class<?> baseJavaClass, Class<?> superJavaClass,
            Consumer<EClass> consumer) {
        if (superJavaClass.getEnclosingClass() != null) {
            mappingSession.mapChild(new SuperClassRequest(baseJavaClass, superJavaClass), superJavaClass,
                    this::buildObjectType, consumer);
        }
    }

    private <E extends ENamedElement, J extends AnnotatedElement> Function<J, E> wrapBuilder(
            MappingSession mappingSession, BiFunction<MappingSession, J, E> builder) {
        return (javaElement) -> {
            E built = builder.apply(mappingSession, javaElement);
            // System.out.println(toString(javaElement, built));
            return built;
        };
    }

    private void addAttributes(MappingSession mappingSession, Class<?> clazz, EClass eClass) {
        final Stream<Method> attributeAccessors = getMethodsWithAnnotation(clazz, Meta.Attribute.class);
        Stream<EAttribute> eAttributes = attributeAccessors.map(wrapBuilder(mappingSession, this::buildAttribute));
        eAttributes.forEach(debug("adding attribute to " + eClass.getName() + " (" + eClass + ")",
                eClass.getEStructuralFeatures()::add));
    }

    private void addContainments(MappingSession mappingSession, Class<?> clazz, EClass eClass) {
        doAddReferences(mappingSession, clazz, eClass, Meta.Contained.class,
                wrapBuilder(mappingSession, this::buildContainmentReference));
    }

    private void addReferences(MappingSession mappingSession, Class<?> clazz, EClass eClass) {
        doAddReferences(mappingSession, clazz, eClass, Meta.Reference.class,
                wrapBuilder(mappingSession, this::buildReference));
    }

    private void doAddReferences(MappingSession mappingSession, Class<?> clazz, EClass eClass,
            Class<? extends Annotation> annotationClass, Function<Method, EReference> builder) {
        Stream<Method> accessors = getMethodsWithAnnotation(clazz, annotationClass);
        List<EReference> references = accessors.map(builder).collect(Collectors.toList());
        references.forEach(debug("Adding reference to " + eClass.getName(), eClass.getEStructuralFeatures()::add));
    }

    private Stream<Method> getMethodsWithAnnotation(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        return Arrays.stream(clazz.getDeclaredMethods()).filter(m -> m.getAnnotation(annotationClass) != null)
                .sorted(Comparator.comparing(Method::getName));
    }

    private Consumer<EReference> referenceConnector(String oppositeName) {
        return StringUtils.isEmpty(oppositeName) ? r -> {
        } : thisSide -> {
            EReference opposite = thisSide.getEReferenceType().getEReferences().stream()
                    .filter(r -> oppositeName.equals(r.getName())).findFirst().get();
            markAsOpposite(thisSide, opposite);
        };
    }

    private void markAsOpposite(EReference thisSide, EReference opposite) {
        thisSide.setEOpposite(opposite);
        opposite.setEOpposite(thisSide);
        // System.out.println("Marked as opposite: " + opposite + "\n\tof " + thisSide);
    }

    private EReference buildReference(MappingSession mappingSession, Method accessor) {
        String oppositeName = MetaEcoreHelper
                .getAnnotationValue(accessor, Meta.Reference.class, Meta.Reference::opposite).get();
        return doBuildReference(mappingSession, accessor, referenceConnector(oppositeName));
    }

    private EReference buildContainmentReference(MappingSession mappingSession, Method accessor) {
        return doBuildReference(mappingSession, accessor, this::markReferenceAsContainment);
    }

    private void markReferenceAsContainment(EReference reference) {
        reference.setContainment(true);
        // System.out.println("Marked as containment: " + reference);
    }

    private EReference doBuildReference(MappingSession mappingSession, Method accessor,
            Consumer<EReference> customizer) {
        EReference eReference = EcoreFactory.eINSTANCE.createEReference();
        eReference.setName(accessor.getName());
        markOptional(accessor, eReference);
        Consumer<EClass> typeSetter = eReference::setEType;
        mappingSession.mapChild(accessor, MetaEcoreHelper.getType(accessor), this::buildObjectType,
                debug("Setting reference type on " + eReference.getName(),
                        typeSetter.andThen(resolvedEClass -> customizer.accept(eReference))));
        return eReference;
    }

    private EAttribute buildAttribute(MappingSession mappingSession, Method accessor) {
        EAttribute eAttribute = EcoreFactory.eINSTANCE.createEAttribute();
        String attributeName = WordUtils.uncapitalize(accessor.getName().replaceFirst("^get", ""));
        eAttribute.setName(attributeName);
        markOptional(accessor, eAttribute);
        buildIfNeeded(MetaEcoreHelper.getType(accessor), mappingSession, debug(
                "Setting type of attribute " + eAttribute.getName(), setAttributeType(mappingSession, eAttribute)));
        return eAttribute;
    }

    private <EC extends EClassifier> Consumer<EC> setAttributeType(MappingSession mappingSession,
            EAttribute attribute) {
        return classifier -> {
            attribute.setEType(classifier);
            String requestId = attribute.getName() + " : " + classifier.getName();
            mappingSession.addPendingRequest(requestId::toString, "setAttributeType(" + attribute.getName() + ")",
                    false, ctx -> {
                        EClass eContainingClass = attribute.getEContainingClass();
                        EPackage containingClassEPackage = eContainingClass.getEPackage();
                        EList<EClassifier> containingClassEPackageClassifiers = containingClassEPackage
                                .getEClassifiers();
                        containingClassEPackageClassifiers.add(classifier);
                    });
        };
    }

    private <EC extends ENamedElement> Consumer<EC> debug(String tag, Consumer<EC> toDebug) {
        return (value) -> {
            System.out.println(tag + " - " + value);
            toDebug.accept(value);
        };
    }

    private void markOptional(Method accessor, EStructuralFeature feature) {
        Optional<Boolean> required = MetaEcoreHelper.getAnnotationValue(accessor, Meta.Required.class, Required::value);
        boolean multivalued = Iterable.class.isAssignableFrom(accessor.getReturnType())
                || Array.class.isAssignableFrom(accessor.getReturnType());
        if (multivalued) {
            feature.setLowerBound(required.map(v -> v ? 1 : 0).orElse(0));
            feature.setUpperBound(ETypedElement.UNBOUNDED_MULTIPLICITY);
        } else {
            feature.setLowerBound(required.map(v -> v ? 1 : 0).orElse(1));
            feature.setUpperBound(1);
        }
    }

//	private EClassifier buildBasicType(MappingSession mappingSession, Class<?> type) {
//		if (type.isEnum())
//			return buildEnumType(mappingSession, type);
//		if (MetaEcoreHelper.isRecord(type))
//			return buildRecordType(mappingSession, type);
//		return buildPrimitiveType(mappingSession, type);
//
////		if (type == String.class) {
////			return EcorePackage.eINSTANCE.getEString();
////		}
////		if (type == int.class) {
////			return EcorePackage.eINSTANCE.getEInt();
////		}
////		if (type == Integer.class) {
////			return EcorePackage.eINSTANCE.getEIntegerObject();
////		}
////		if (type == boolean.class) {
////			return EcorePackage.eINSTANCE.getEBoolean();
////		}
////		if (type == Boolean.class) {
////			return EcorePackage.eINSTANCE.getEBooleanObject();
////		}
////		EDataType dataType = EcoreFactory.eINSTANCE.createEDataType();
////		dataType.setInstanceTypeName(type.getName());
////		dataType.setName(type.getSimpleName());
////		return dataType;
//	}

//	private EDataType getPrimitiveType(Class<?> javaPrimitiveType) {
//		if (javaPrimitiveType == String.class) {
//			return EcorePackage.eINSTANCE.getEString();
//		}
//		if (javaPrimitiveType == int.class) {
//			return EcorePackage.eINSTANCE.getEInt();
//		}
//		if (javaPrimitiveType == Integer.class) {
//			return EcorePackage.eINSTANCE.getEIntegerObject();
//		}
//		if (javaPrimitiveType == boolean.class) {
//			return EcorePackage.eINSTANCE.getEBoolean();
//		}
//		if (javaPrimitiveType == Boolean.class) {
//			return EcorePackage.eINSTANCE.getEBooleanObject();
//		}
//		if (MetaEcoreHelper.isPrimitive(javaPrimitiveType)) {
//
//		}
//		throw new IllegalStateException(javaPrimitiveType.getSimpleName());
//	}

}
