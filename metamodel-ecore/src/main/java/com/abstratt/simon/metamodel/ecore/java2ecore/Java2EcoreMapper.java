package com.abstratt.simon.metamodel.ecore.java2ecore;

import com.abstratt.simon.metamodel.dsl.Meta;
import com.abstratt.simon.metamodel.dsl.Meta.Required;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.ETypedElement;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreUtil;


/**
 * Builds an EMF-based metamodel from Java classes that have been marked with Simon DSL annotations
 * as defined in {@link Meta}.
 */
public class Java2EcoreMapper {
	/**
	 * Maps the given Java class that utilizes Simon's annotation-based DSL to an EMF-based metamodel.
	 */
	public <E extends EObject> E map(Class<?> clazz) {
		//System.out.println("*** " + Arrays.stream(new Throwable().fillInStackTrace().getStackTrace()).skip(1)
		//		.findFirst().get().toString());

		Context context = new Context();
		Object[] result = { null };
		buildIfNeeded(clazz, context, r -> result[0] = r);
		//System.out.println("**** Resolving pending requests");
		context.resolvePendingRequests();
		//System.out.println("**** Done");

		return (E) result[0];
	}

	/**
	 * Builds the model element corresponding to the given Java type, if needed.
	 * 
	 * @param <E>      the concrete model element
	 * @param clazz    the Java type
	 * @param context  the resolution context
	 * @param consumer a consumer for the built or resolved element
	 */
	private <E extends ENamedElement> void buildIfNeeded(Class<?> clazz, Context context, Consumer<E> consumer) {
		if (MetaEcoreHelper.isPackage(clazz)) {
			context.resolveRoot(null, clazz, this::buildPackage, (Consumer<EPackage>) consumer);
			return;
		}
		// may need to build the package first
		Class<?> packageClass = getPackageClass(clazz);

		context.resolve(new Context.ResolutionAction<>(null, packageClass,
				packageElement -> buildIfNeeded(packageElement, clazz, context, consumer), //
				this::buildPackage, //
				true));
	}

	private <E extends ENamedElement> void buildIfNeeded(EPackage parentPackage, Class<?> clazz, Context context,
														 Consumer<E> consumer) {
		assert parentPackage != null : clazz.getName();
		context.runWithScope(parentPackage, "findOrBuild(" + clazz.getName() + ")", ctx -> {
			if (MetaEcoreHelper.isPrimitive(clazz)) {
				if (clazz.isEnum()) {
//					Enum<?>[] enumConstants = (Enum<?>[]) clazz.getEnumConstants();
//					for (Enum<?> enum1 : enumConstants) {
//						System.out.println("Primitive: " + enum1.name());
//						context.resolve(clazz, clazz, this::buildPrimitiveType, (Consumer<EClass>) consumer);			
//					}	
				} else {
					context.resolve(clazz, clazz, this::buildPrimitiveType, (Consumer<EClass>) consumer);
				}

			} else if (MetaEcoreHelper.isEnum(clazz)) {
				context.resolve(clazz, clazz, this::buildEnumType, (Consumer<EEnum>) consumer);
			} else if (MetaEcoreHelper.isRecord(clazz)) {
				context.resolve(clazz, clazz, this::buildRecordType, (Consumer<EClass>) consumer);
			} else {
				context.resolve(clazz, clazz, this::buildObjectType, (Consumer<EClass>) consumer);
			}

		});
	}

	private static <J extends AnnotatedElement, E extends ENamedElement> String toString(J javaElement, E result) {
		return javaElement + " => " + toString(result);
	}

	private static <E extends ENamedElement> String toString(E result) {
		return result.getName() + " (" + result.eClass().getName() + ")";
	}

	private EPackage buildPackage(Context context, Class<?> packageClass) {
		EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
		ePackage.setNsURI(packageClass.getSimpleName());
		ePackage.setNsPrefix(packageClass.getSimpleName());
		ePackage.setName(packageClass.getSimpleName());

		var packageAnnotation = packageClass.getAnnotation(Meta.Package.class);
		var builtIns = packageAnnotation.builtIns();
		storeBuiltIns(packageClass, ePackage, builtIns);

		context.runWithScope(ePackage, "Building classes in package " + packageClass.getName(), ctx -> {
			for (Class<?> nested : packageClass.getClasses()) {
				this.<EClassifier>buildIfNeeded(nested, context, ePackage.getEClassifiers()::add);
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

//	private EDataType buildPrimitiveType(Context context, Class<?> clazz) {
//		String className = clazz.getSimpleName();
//		System.out.println("Building primitive type " + className);
//		EDataType eDataType = EcoreFactory.eINSTANCE.createEDataType();
//		eDataType.setName(className);
//		eDataType.setInstanceTypeName(className);
//		eDataType.setInstanceClass(clazz);
//		return eDataType;
//	}

	private EClass buildPrimitiveType(Context context, Class<?> clazz) {
		// a primitive type is a class without any features
		String className = clazz.getSimpleName();
		//System.out.println("Building primitive type " + className);
		EClass eClass = EcoreFactory.eINSTANCE.createEClass();
		eClass.setName(className);
		EAttribute eAttribute = EcoreFactory.eINSTANCE.createEAttribute();
		eAttribute.setName(MetaEcoreHelper.PRIMITIVE_VALUE_FEATURE);
		EDataType primitiveType = MetaEcoreHelper.getPrimitiveEType(clazz);
		eAttribute.setEType(primitiveType);
		eClass.getEStructuralFeatures().add(eAttribute);
		MetaEcoreHelper.makePrimitiveType(eClass, MetaEcoreHelper.getPrimitiveKind(primitiveType));
		addSupertypes(context, clazz, eClass);
		return eClass;
	}

	private EEnum buildEnumType(Context context, Class<?> clazz) {
		String className = clazz.getSimpleName();
		//System.out.println("Building enum type " + className);
		EEnum eEnum = EcoreFactory.eINSTANCE.createEEnum();
		eEnum.setName(className);
		Object[] enumConstants = clazz.getEnumConstants();
		for (Object object : enumConstants) {
			EEnumLiteral literal = EcoreFactory.eINSTANCE.createEEnumLiteral();
			Enum asJavaEnum = (Enum) object;
			String valueAsStr = asJavaEnum.name();
			literal.setName(valueAsStr);
			literal.setLiteral(valueAsStr);
			literal.setValue((asJavaEnum.ordinal()));
			eEnum.getELiterals().add(literal);
		}
		return eEnum;
	}

	private EClass buildRecordType(Context context, Class<?> clazz) {
		String className = clazz.getSimpleName();
		//System.out.println("Building record type " + className);
		EClass eClass = EcoreFactory.eINSTANCE.createEClass();
		eClass.setName(className);
		MetaEcoreHelper.makeRecordType(eClass);
		addClassifierToCurrentPackage(context, eClass);
		addSupertypes(context, clazz, eClass);
		addAttributes(context, clazz, eClass);
		return eClass;
	}

	private EClass buildObjectType(Context context, Class<?> clazz) {
		String className = clazz.getSimpleName();
		//System.out.println("Building object type " + className);
		boolean isInterface = Modifier.isInterface(clazz.getModifiers());
		boolean isAbstractClass = Modifier.isAbstract(clazz.getModifiers());
		boolean isRootComposite = MetaEcoreHelper.isRootComposite(clazz);
		EClass eClass = EcoreFactory.eINSTANCE.createEClass();
		if (isRootComposite) {
			EcoreUtil.setAnnotation(eClass, MetaEcoreHelper.SIMON_ANNOTATION, MetaEcoreHelper.ROOT_COMPOSITE_VALUE,
					Boolean.toString(true));
		}
		addSupertypes(context, clazz, eClass);

		eClass.setName(className);
		eClass.setAbstract(isAbstractClass);
		eClass.setInterface(isInterface);
		addClassifierToCurrentPackage(context, eClass);
		addContainments(context, clazz, eClass);
		addReferences(context, clazz, eClass);
		addAttributes(context, clazz, eClass);
		return eClass;
	}

	private void addSupertypes(Context context, Class<?> clazz, EClass eClass) {
		Class<?> superJavaClass = clazz.getSuperclass();
		if (superJavaClass != null) {
			collectSuperType(context, clazz, superJavaClass,
					debug("Setting super type on " + eClass.getName(), eClass.getESuperTypes()::add));
		}
		for (Class<?> i : clazz.getInterfaces()) {
			collectSuperType(context, clazz, i,
					debug("Setting interface as super type on " + eClass.getName(), eClass.getESuperTypes()::add));
		}
	}

	private void addClassifierToCurrentPackage(Context context, EClass eClass) {
		assert context.inScope();
		var parent = context.currentScope();
		assert parent != null;
		assert parent instanceof EPackage;
		((EPackage) parent).getEClassifiers().add(eClass);
	}

	private Class<?> getPackageClass(Class<?> containedClazz) {
		assert containedClazz.isMemberClass();
		return containedClazz.getEnclosingClass();
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
			} else return classToResolve.equals(other.classToResolve);
		}

	}

	private void collectSuperType(Context context, Class<?> baseJavaClass, Class<?> superJavaClass,
			Consumer<EClass> consumer) {
		if (superJavaClass != Object.class) {
			context.resolve(new SuperClassRequest(baseJavaClass, superJavaClass), superJavaClass, this::buildObjectType,
					consumer);
		}
	}

	private <E extends ENamedElement, J extends AnnotatedElement> Function<J, E> wrapBuilder(Context context,
			BiFunction<Context, J, E> builder) {
		return (javaElement) -> {
			E built = builder.apply(context, javaElement);
			//System.out.println(toString(javaElement, built));
			return built;
		};
	}

	private void addAttributes(Context context, Class<?> clazz, EClass eClass) {
		final Stream<Method> attributeAccessors = getMethodsWithAnnotation(clazz, Meta.Attribute.class);
		Stream<EAttribute> eAttributes = attributeAccessors.map(wrapBuilder(context, this::buildAttribute));
		eAttributes.forEach(debug("adding attribute to " + eClass.getName(), eClass.getEStructuralFeatures()::add));
	}

	private void addContainments(Context context, Class<?> clazz, EClass eClass) {
		doAddReferences(context, clazz, eClass, Meta.Contained.class,
				wrapBuilder(context, this::buildContainmentReference));
	}

	private void addReferences(Context context, Class<?> clazz, EClass eClass) {
		doAddReferences(context, clazz, eClass, Meta.Reference.class, wrapBuilder(context, this::buildReference));
	}

	private void doAddReferences(Context context, Class<?> clazz, EClass eClass,
			Class<? extends Annotation> annotationClass, Function<Method, EReference> builder) {
		Stream<Method> accessors = getMethodsWithAnnotation(clazz, annotationClass);
		List<EReference> references = accessors.map(builder).collect(Collectors.toList());
		references.forEach(eClass.getEStructuralFeatures()::add);
	}

	private Stream<Method> getMethodsWithAnnotation(Class<?> clazz, Class<? extends Annotation> annotationClass) {
		return Arrays.stream(clazz.getDeclaredMethods()).filter(m -> m.getAnnotation(annotationClass) != null)
				.sorted((m1, m2) -> m1.getName().compareTo(m2.getName()));
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
		//System.out.println("Marked as opposite: " + opposite + "\n\tof " + thisSide);
	}

	private EReference buildReference(Context context, Method accessor) {
		String oppositeName = getAnnotationValue(accessor, Meta.Reference.class, Meta.Reference::opposite).get();
		return doBuildReference(context, accessor, referenceConnector(oppositeName));
	}

	private EReference buildContainmentReference(Context context, Method accessor) {
		return doBuildReference(context, accessor, reference -> markReferenceAsContainment(reference));
	}

	private void markReferenceAsContainment(EReference reference) {
		reference.setContainment(true);
		//System.out.println("Marked as containment: " + reference);
	}

	private EReference doBuildReference(Context context, Method accessor, Consumer<EReference> customizer) {
		EReference eReference = EcoreFactory.eINSTANCE.createEReference();
		eReference.setName(accessor.getName());
		markOptional(accessor, eReference);
		Consumer<EClass> typeSetter = eReference::setEType;
		context.resolve(accessor, getType(accessor), this::buildObjectType,
				debug("Setting reference type on " + eReference.getName(),
						typeSetter.andThen(resolvedEClass -> customizer.accept(eReference))));
		return eReference;
	}

	private EAttribute buildAttribute(Context context, Method accessor) {
		EAttribute eAttribute = EcoreFactory.eINSTANCE.createEAttribute();
		String attributeName = WordUtils.uncapitalize(accessor.getName().replaceFirst("^get", ""));
		eAttribute.setName(attributeName);
		markOptional(accessor, eAttribute);
		context.resolve(accessor, getType(accessor), this::buildBasicType,
				debug("Setting type of attribute " + eAttribute.getName(), setAttributeType(context, eAttribute)));
		return eAttribute;
	}

	private <EC extends EClassifier> Consumer<EC> setAttributeType(Context context, EAttribute attribute) {
		return classifier -> {
			attribute.setEType(classifier);
			String requestId = attribute.getName() + " : " + classifier.getName();
			context.addPendingRequest(requestId::toString, "setAttributeType(" + attribute.getName() + ")", false,
					ctx -> {
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
			//System.out.println(tag + " - " + value);
			toDebug.accept(value);
		};
	}

	private void markOptional(Method accessor, EStructuralFeature feature) {
		Optional<Boolean> required = getAnnotationValue(accessor, Meta.Required.class, Required::value);
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

	private EClassifier buildBasicType(Context context, Class<?> type) {
		if (type.isEnum())
			return buildEnumType(context, type);
		if (MetaEcoreHelper.isRecord(type))
			return buildRecordType(context, type);
		return buildPrimitiveType(context, type);

//		if (type == String.class) {
//			return EcorePackage.eINSTANCE.getEString();
//		}
//		if (type == int.class) {
//			return EcorePackage.eINSTANCE.getEInt();
//		}
//		if (type == Integer.class) {
//			return EcorePackage.eINSTANCE.getEIntegerObject();
//		}
//		if (type == boolean.class) {
//			return EcorePackage.eINSTANCE.getEBoolean();
//		}
//		if (type == Boolean.class) {
//			return EcorePackage.eINSTANCE.getEBooleanObject();
//		}
//		EDataType dataType = EcoreFactory.eINSTANCE.createEDataType();
//		dataType.setInstanceTypeName(type.getName());
//		dataType.setName(type.getSimpleName());
//		return dataType;
	}

	private EDataType getPrimitiveType(Class<?> javaPrimitiveType) {
		if (javaPrimitiveType == String.class) {
			return EcorePackage.eINSTANCE.getEString();
		}
		if (javaPrimitiveType == int.class) {
			return EcorePackage.eINSTANCE.getEInt();
		}
		if (javaPrimitiveType == Integer.class) {
			return EcorePackage.eINSTANCE.getEIntegerObject();
		}
		if (javaPrimitiveType == boolean.class) {
			return EcorePackage.eINSTANCE.getEBoolean();
		}
		if (javaPrimitiveType == Boolean.class) {
			return EcorePackage.eINSTANCE.getEBooleanObject();
		}
		if (MetaEcoreHelper.isPrimitive(javaPrimitiveType)) {

		}
		throw new IllegalStateException(javaPrimitiveType.getSimpleName());
	}

	private Class<?> getType(Method method) {
		Optional<Class<?>> explicitType = getType((AnnotatedElement) method);
		return explicitType.orElseGet(method::getReturnType);
	}

	private Optional<Class<?>> getType(AnnotatedElement it) {
		return getAnnotationValue(it, Meta.Typed.class, Meta.Typed::value);

	}

	private <A extends Annotation, V> Optional<V> getAnnotationValue(AnnotatedElement it, Class<A> annotationClass,
			Function<A, V> mapper) {
		A annotation = it.getAnnotation(annotationClass);
		return Optional.ofNullable(annotation).map(mapper);
	}
}
