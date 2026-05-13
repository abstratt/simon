package com.abstratt.simon.metamodel.ecore.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.ETypedElement;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreUtil;

import com.abstratt.simon.metamodel.Metamodel;
import com.abstratt.simon.metamodel.Metamodel.PrimitiveKind;
import com.abstratt.simon.metamodel.dsl.Meta;
import com.abstratt.simon.metamodel.dsl.Meta.Type;

/**
 * Simon's conventions layered on top of Ecore: the mapping between the
 * {@link Meta @Meta.*} annotation DSL, marker
 * {@link org.eclipse.emf.ecore.EAnnotation}s, and the roles features and
 * classifiers can play.
 *
 * <p>Owns the {@value #SIMON_ANNOTATION} annotation source and the tag
 * vocabulary stored under it ({@value #ROOT_COMPOSITE_VALUE}, {@link #NAME_FEATURE},
 * {@link #DOCUMENTATION_FEATURE}, etc.), as well as the {@value #PRIMITIVE_VALUE_FEATURE}
 * convention used to back wrapped primitive values.
 *
 * <p>Members fall into four groups:
 * <ul>
 *   <li><b>Predicates</b> over Java classes and Ecore classifiers —
 *       {@link #isPackage}, {@link #isObjectType}, {@link #isPrimitive},
 *       {@link #isRecord}, {@link #isRootComposite}, {@link #isNamed}.</li>
 *   <li><b>Marker writers</b> used during metamodel construction —
 *       {@link #makeRootComposite}, {@link #makeRecordType},
 *       {@link #makePrimitiveType}, {@link #markAsName},
 *       {@link #markAsDocumentation}.</li>
 *   <li><b>Role lookups</b> that find the structural feature playing a
 *       given role on an {@link EClass} —
 *       {@link #getNameAttribute}, {@link #getDocumentationAttribute},
 *       {@link #getValueFeature}.</li>
 *   <li><b>Java-to-Ecore primitive bridging</b> —
 *       {@link #getPrimitiveKind}, {@link #getPrimitiveEType},
 *       {@link #getPrimitiveETypeOrNull}.</li>
 * </ul>
 *
 * <p>Inputs are typically metamodel-layer entities: an {@link EClass},
 * an {@link EAttribute}, or a Java {@code Class<?>} authored with the
 * {@code @Meta} DSL. Per-instance state of model objects is out of scope.
 */
public interface MetaEcoreHelper {
    String ROOT_COMPOSITE_VALUE = "rootComposite";
    String PRIMITIVE_TYPE_KIND = "kind";
    String PRIMITIVE_TYPE = Meta.PrimitiveType.class.getSimpleName();
    String RECORD_TYPE = Meta.RecordType.class.getSimpleName();
    String SIMON_ANNOTATION = "simon.annotations";
    String PRIMITIVE_VALUE_FEATURE = "__value__";
    String NAME_FEATURE = "name";
    String DOCUMENTATION_FEATURE = "documentation";
    String MODIFIER_FEATURE = "modifier";

    static void makeRootComposite(EClass eClass) {
        EcoreUtil.setAnnotation(eClass, MetaEcoreHelper.SIMON_ANNOTATION, MetaEcoreHelper.ROOT_COMPOSITE_VALUE,
                Boolean.toString(true));
    }

    static void makeRecordType(EClass eClass) {
        EcoreUtil.setAnnotation(eClass, MetaEcoreHelper.SIMON_ANNOTATION, MetaEcoreHelper.RECORD_TYPE,
                Boolean.toString(true));
    }

    static void makePrimitiveType(EClass eClass, Metamodel.PrimitiveKind kind) {
        setAnnotation(eClass, MetaEcoreHelper.PRIMITIVE_TYPE, Boolean.toString(true));
        setAnnotation(eClass, MetaEcoreHelper.PRIMITIVE_TYPE_KIND, kind.name());
    }

    static void setAnnotation(EClass eClass, String tag, String value) {
        EcoreUtil.setAnnotation(eClass, MetaEcoreHelper.SIMON_ANNOTATION, tag, value);
    }

    static boolean isRootComposite(EClass eClass) {
        String tag = MetaEcoreHelper.ROOT_COMPOSITE_VALUE;
        return Boolean.TRUE.toString().equals(getAnnotation(eClass, tag));
    }

    static String getAnnotation(EModelElement eClass, String tag) {
        return EcoreUtil.getAnnotation(eClass, MetaEcoreHelper.SIMON_ANNOTATION, tag);
    }

    static boolean isPrimitive(EClassifier eClass) {
        var tag = MetaEcoreHelper.PRIMITIVE_TYPE;
        return Boolean.TRUE.toString().equals(getAnnotation(eClass, tag));
    }

    static Metamodel.PrimitiveKind getPrimitiveKind(EClassifier eClass) {
        if (eClass == EcorePackage.eINSTANCE.getEString())
            return PrimitiveKind.String;
        if (eClass == EcorePackage.eINSTANCE.getEInt() || eClass == EcorePackage.eINSTANCE.getEIntegerObject())
            return PrimitiveKind.Integer;
        if (eClass == EcorePackage.eINSTANCE.getEBoolean() || eClass == EcorePackage.eINSTANCE.getEBooleanObject())
            return PrimitiveKind.Boolean;
        if (eClass == EcorePackage.eINSTANCE.getEBigDecimal())
            return PrimitiveKind.Decimal;

        throw new IllegalArgumentException(eClass.getName());
    }

    static boolean isRecord(EClassifier eClass) {
        String tag = MetaEcoreHelper.RECORD_TYPE;
        return Boolean.TRUE.toString().equals(getAnnotation(eClass, tag));
    }

    static boolean isPackage(Class<?> clazz) {
        return clazz.isAnnotationPresent(Meta.Package.class);
    }

    static boolean isObjectType(Class<?> clazz) {
        return isExplicitlyObjectType(clazz) || isImplicitlyObjectType(clazz);
    }

    static boolean isImplicitlyObjectType(Class<?> clazz) {
        var typeAnnotations = getTypeAnnotations(clazz);
        return typeAnnotations.findAny().isEmpty();
    }

    static Stream<Annotation> getTypeAnnotations(Class<?> clazz) {
        var annotations = clazz.getAnnotations();
        var typeAnnotations = Arrays.stream(annotations).filter(MetaEcoreHelper::isTypeAnnotation);
        return typeAnnotations;
    }

    static boolean isTypeAnnotation(Annotation ann) {
        var typeAnnotations = getAnnotationTypes(ann);
        return typeAnnotations.length > 0;
    }

    static Type[] getAnnotationTypes(Annotation ann) {
        var typeAnnotations = ann.annotationType().getAnnotationsByType(Type.class);
        return typeAnnotations;
    }

    static boolean isExplicitlyObjectType(Class<?> clazz) {
        return clazz.isAnnotationPresent(Meta.ObjectType.class);
    }

    static boolean isEnum(Class<?> clazz) {
        return clazz.isEnum();
    }

    static boolean isRecord(Class<?> clazz) {
        return clazz.isAnnotationPresent(Meta.RecordType.class);
    }

    static boolean isPrimitive(Class<?> clazz) {
        return clazz.isAnnotationPresent(Meta.PrimitiveType.class);
    }

    static boolean isPrimitiveJavaClass(Class<?> clazz) {
        return getPrimitiveETypeOrNull(clazz) != null;
    }

    static PrimitiveKind getPrimitiveKind(Class<?> clazz) {
        assert isPrimitive(clazz) : clazz;
        return clazz.getAnnotation(Meta.PrimitiveType.class).value();
    }

    static EDataType getPrimitiveEType(PrimitiveKind primitiveKind) {
        var eDataType = getPrimitiveETypeOrNull(primitiveKind);
        if (eDataType == null)
            throw new IllegalArgumentException(primitiveKind.name());
        return eDataType;
    }

    static EDataType getPrimitiveETypeOrNull(PrimitiveKind primitiveKind) {
        return switch (primitiveKind) {
        case Boolean -> EcorePackage.Literals.EBOOLEAN;
        case Integer -> EcorePackage.Literals.EINT;
        case Decimal -> EcorePackage.Literals.EBIG_DECIMAL;
        case String, Other -> EcorePackage.Literals.ESTRING;
        default -> null;
        };
    }

    static EDataType getPrimitiveEType(Class<?> clazz) {
        var primitiveETypeOrNull = getPrimitiveETypeOrNull(clazz);
        if (primitiveETypeOrNull == null)
            throw new IllegalArgumentException(clazz.getName());
        return primitiveETypeOrNull;
    }

    static EDataType getPrimitiveETypeOrNull(Class<?> clazz) {
        if (isPrimitive(clazz))
            return getPrimitiveETypeOrNull(getPrimitiveKind(clazz));
        if (clazz == Boolean.class)
            return EcorePackage.Literals.EBOOLEAN_OBJECT;
        if (clazz == boolean.class)
            return EcorePackage.Literals.EBOOLEAN;
        if (clazz == Integer.class)
            return EcorePackage.Literals.EINTEGER_OBJECT;
        if (clazz == int.class)
            return EcorePackage.Literals.EINT;
        if (clazz == String.class)
            return EcorePackage.Literals.ESTRING;

        return null;
    }

    static boolean isRootComposite(Class<?> clazz) {
        return clazz.isAnnotationPresent(Meta.Composite.class) && clazz.getAnnotation(Meta.Composite.class).root();
    }

    static boolean isComposite(Class<?> clazz) {
        return clazz.isAnnotationPresent(Meta.Composite.class);
    }

    static EStructuralFeature getValueFeature(EClass labelType) {
        EStructuralFeature valueFeature = labelType.getEStructuralFeature(MetaEcoreHelper.PRIMITIVE_VALUE_FEATURE);
        return valueFeature;
    }

    static EDataType getValueType(ETypedElement name) {
        return (EDataType) MetaEcoreHelper.getValueFeature((EClass) name.getEType()).getEType();
    }

    static EAttribute getNameAttribute(EObject named) {
        EClass eClass = named.eClass();
        return getNameAttribute(eClass);
    }

    static EAttribute getNameAttribute(EClass eClass) {
        return findMarkedAttribute(eClass, NAME_FEATURE);
    }

    static void markAsName(EAttribute attribute) {
        EcoreUtil.setAnnotation(attribute, SIMON_ANNOTATION, NAME_FEATURE, Boolean.toString(true));
    }

    static void markAsDocumentation(EAttribute attribute) {
        EcoreUtil.setAnnotation(attribute, SIMON_ANNOTATION, DOCUMENTATION_FEATURE, Boolean.toString(true));
    }

    static void markAsModifier(EAttribute attribute) {
        EcoreUtil.setAnnotation(attribute, SIMON_ANNOTATION, MODIFIER_FEATURE, Boolean.toString(true));
    }

    static boolean isModifier(EAttribute attribute) {
        return Boolean.TRUE.toString().equals(getAnnotation(attribute, MODIFIER_FEATURE));
    }

    static EAttribute getDocumentationAttribute(EClass eClass) {
        return findMarkedAttribute(eClass, DOCUMENTATION_FEATURE);
    }

    private static EAttribute findMarkedAttribute(EClass eClass, String tag) {
        return eClass.getEAllAttributes().stream()
                .filter(a -> Boolean.TRUE.toString().equals(getAnnotation(a, tag)))
                .findFirst().orElse(null);
    }

    static boolean isNamed(EClass eClass) {
        return getNameAttribute(eClass) != null;
    }

    static boolean isNamed(EObject eObject) {
        return getNameAttribute(eObject) != null;
    }

    public static Class<?> getType(Method method) {
        Optional<Class<?>> explicitType = getType((AnnotatedElement) method);
        var actualType = explicitType.orElseGet(method::getReturnType);
        return actualType;
    }

    public static Optional<Class<?>> getType(AnnotatedElement it) {
        return getAnnotationValue(it, Meta.Typed.class, Meta.Typed::value);
    }

    public static <A extends Annotation, V> Optional<V> getAnnotationValue(AnnotatedElement it,
            Class<A> annotationClass, Function<A, V> mapper) {
        A annotation = it.getAnnotation(annotationClass);
        return Optional.ofNullable(annotation).map(mapper);
    }

}
