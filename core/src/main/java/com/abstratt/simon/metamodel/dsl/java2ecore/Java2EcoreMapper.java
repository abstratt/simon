package com.abstratt.simon.metamodel.dsl.java2ecore;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
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

import com.abstratt.simon.metamodel.dsl.Meta;
import com.abstratt.simon.metamodel.dsl.Meta.PrimitiveType;
import com.abstratt.simon.metamodel.dsl.Meta.RecordType;
import com.abstratt.simon.metamodel.dsl.Meta.Required;

public class Java2EcoreMapper {
	private class Context {
		private Set<Object> building = new LinkedHashSet<>();
		private Set<Object> served = new LinkedHashSet<>();
		private Deque<Runnable> pendingRequests = new LinkedList<>();
		private Map<Class<?>, ENamedElement> built = new LinkedHashMap<>();
		private AtomicInteger counter = new AtomicInteger();

		public <EC extends ENamedElement> void resolve(Class<?> type, BiFunction<Context, Class<?>, EC> builder,
				Consumer<EC> consumer) {
			int requestId = counter.incrementAndGet();
			// we are not resolving for an element that needs the type, so use a unique id
			this.resolve(requestId, type, builder, consumer);
		}

		/**
		 * Requests a type to be resolved in the context of mapping a Java element to a
		 * model element.
		 * 
		 * @param requestId      that Java element that we are mapping and needs the resolution
		 * @param classToResolve The Java class we need to resolve to a model element
		 * @param builder        a builder function that can, if necessary, map the Java
		 *                       type to a model element
		 * @param consumer       a function that will process the resulting model
		 *                       element
		 */
		public <EC extends ENamedElement> void resolve(Object requestId, Class<?> classToResolve,
				BiFunction<Context, Class<?>, EC> builder, Consumer<EC> consumer) {
			System.out.println("Accepted request for " + requestId + " -> " + classToResolve.getSimpleName());
			EC existing = (EC) built.get(classToResolve);
			if (existing != null) {
				serve(requestId, consumer, existing);
				return;
			}
			// Map the type asynchronously
			Runnable[] task = { null };
			task[0] = () -> {
				System.out.println("Async handling request " + requestId + " -> " + classToResolve.getSimpleName());
				if (isServed(requestId)) {
					// We accept multiple requests
					System.out.println("Already served");
					return;
				}
				EC existingElement = (EC) built.get(classToResolve);
				if (existingElement != null) {
					System.out.println("Already solved");
					serve(requestId, consumer, existingElement);
				} else {
					if (building.add(classToResolve)) {
						System.out.println("Building for " + requestId + " -> " + classToResolve.getSimpleName());
						EC newElement = builder.apply(this, classToResolve);
						built.put(classToResolve, newElement);
						System.out.println("Built for " + requestId + " -> " + classToResolve.getSimpleName());
						serve(requestId, consumer, newElement);
					} else {
						System.out.println("Re-scheduling request " + requestId + " -> " + classToResolve.getSimpleName());
						pendingRequests.add(task[0]);
					}
				}
			};
			System.out.println("Scheduling request " + requestId + " -> " + classToResolve.getSimpleName());
			pendingRequests.add(task[0]);
		}

		private boolean isServed(Object requestId) {
			return served.contains(requestId);
		}

		private <EC extends ENamedElement> void serve(Object requestId, Consumer<EC> consumer, EC resolved) {
			if (served.add(requestId)) {
				System.out.println("Serving request " + requestId);
				consumer.accept(resolved);
			} else {
				System.out.println("Ignoring request " + requestId);
			}
		}

		public void resolveRequests() {
			while (!pendingRequests.isEmpty()) {
				pendingRequests.removeFirst().run();
			}
		}
	}

	public <E extends EObject> E map(Class<?> clazz) {
		System.out.println("*** " + Arrays.stream(new Throwable().fillInStackTrace().getStackTrace()).skip(1)
				.findFirst().get().toString());

		Context context = new Context();
		Object[] result = { null };
		resolve(clazz, context, r -> result[0] = r);
		System.out.println("**** Resolving pending requests");
		context.resolveRequests();
		System.out.println("**** Done");
		
		return (E) result[0];
	}

	private <E extends ENamedElement> void resolve(Class<?> clazz, Context context, Consumer<E> consumer) {
		System.out.println();
		if (MetaEcoreHelper.isPackage(clazz)) {
			context.resolve(clazz, clazz, this::buildPackage, (Consumer<EPackage>) consumer);
		} else if (MetaEcoreHelper.isPrimitive(clazz)) {
			if (clazz.isEnum()) {
//				Enum<?>[] enumConstants = (Enum<?>[]) clazz.getEnumConstants();
//				for (Enum<?> enum1 : enumConstants) {
//					System.out.println("Primitive: " + enum1.name());
//					context.resolve(clazz, clazz, this::buildPrimitiveType, (Consumer<EClass>) consumer);			
//				}	
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
		for (Class<?> nested : packageClass.getClasses()) {
			this.<EClassifier>resolve(nested, context, ePackage.getEClassifiers()::add);
		}
		return ePackage;
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
		System.out.println("Building primitive type " + className);
		EClass eClass = EcoreFactory.eINSTANCE.createEClass();
		eClass.setName(className);
		EAttribute eAttribute = EcoreFactory.eINSTANCE.createEAttribute();
		eAttribute.setName(MetaEcoreHelper.PRIMITIVE_VALUE_FEATURE);
		EDataType primitiveType = MetaEcoreHelper.getPrimitiveEType(clazz);
		eAttribute.setEType(primitiveType);
		eClass.getEStructuralFeatures().add(eAttribute);
		MetaEcoreHelper.makePrimitiveType(eClass, MetaEcoreHelper.getPrimitiveKind(primitiveType));
		return eClass;
	}


	private EEnum buildEnumType(Context context, Class<?> clazz) {
		String className = clazz.getSimpleName();
		System.out.println("Building enum type " + className);
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
		System.out.println("Building record type " + className);
		EClass eClass = EcoreFactory.eINSTANCE.createEClass();
		eClass.setName(className);
		MetaEcoreHelper.makeRecordType(eClass);
		addAttributes(context, clazz, eClass);
		return eClass;
	}

	private EClass buildObjectType(Context context, Class<?> clazz) {
		String className = clazz.getSimpleName();
		System.out.println("Building object type " + className);
		boolean isInterface = Modifier.isInterface(clazz.getModifiers());
		boolean isAbstractClass = Modifier.isAbstract(clazz.getModifiers());
		boolean isRootComposite = MetaEcoreHelper.isRootComposite(clazz);
		EClass eClass = EcoreFactory.eINSTANCE.createEClass();
		if (isRootComposite) {
			EcoreUtil.setAnnotation(eClass, MetaEcoreHelper.SIMON_ANNOTATION, MetaEcoreHelper.ROOT_COMPOSITE_VALUE, Boolean.toString(true));
		}
		Class<?> superJavaClass = clazz.getSuperclass();
		if (superJavaClass != null) {
			collectSuperType(context, clazz, superJavaClass, debug("Setting super type on " + eClass.getName(), eClass.getESuperTypes()::add));
		}
		for (Class<?> i : clazz.getInterfaces()) {
			collectSuperType(context, clazz, i, debug("Setting interface as super type on " + eClass.getName(), eClass.getESuperTypes()::add));
		}

		eClass.setName(className);
		addContainments(context, clazz, eClass);
		addReferences(context, clazz, eClass);
		addAttributes(context, clazz, eClass);
		eClass.setAbstract(isAbstractClass);
		eClass.setInterface(isInterface);
		return eClass;
	}
	
	private static class SuperClassRequest {
		private final Class<?> baseJavaClass;
		private final Class<?> superJavaClass;
		public SuperClassRequest(Class<?> baseJavaClass, Class<?> superJavaClass) {
			this.baseJavaClass = baseJavaClass;
			this.superJavaClass = superJavaClass;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((baseJavaClass == null) ? 0 : baseJavaClass.hashCode());
			result = prime * result + ((superJavaClass == null) ? 0 : superJavaClass.hashCode());
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
			if (superJavaClass == null) {
				if (other.superJavaClass != null)
					return false;
			} else if (!superJavaClass.equals(other.superJavaClass))
				return false;
			return true;
		}
		
	}

	private void collectSuperType(Context context, Class<?> baseJavaClass, Class<?> superJavaClass, Consumer<EClass> consumer) {
		if (superJavaClass != Object.class) {
			context.resolve(new SuperClassRequest(baseJavaClass, superJavaClass), superJavaClass, this::buildObjectType, consumer);
		}
	}

	private <E extends ENamedElement, J extends AnnotatedElement> Function<J, E> wrapBuilder(Context context,
			BiFunction<Context, J, E> builder) {
		return (javaElement) -> {
			E built = builder.apply(context, javaElement);
			System.out.println(toString(javaElement, built));
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
		System.out.println("Marked as opposite: " + opposite + "\n\tof " + thisSide);
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
		System.out.println("Marked as containment: " + reference);
	}

	private EReference doBuildReference(Context context, Method accessor, Consumer<EReference> customizer) {
		EReference eReference = EcoreFactory.eINSTANCE.createEReference();
		eReference.setName(accessor.getName());
		markOptional(accessor, eReference);
		Consumer<EClass> typeSetter = eReference::setEType;
		context.resolve(accessor, getType(accessor), this::buildObjectType,
				debug("Setting reference type on " + eReference.getName(), typeSetter.andThen(resolvedEClass -> customizer.accept(eReference))));
		return eReference;
	}

	private EAttribute buildAttribute(Context context, Method accessor) {
		EAttribute eAttribute = EcoreFactory.eINSTANCE.createEAttribute();
		String attributeName = WordUtils.uncapitalize(accessor.getName().replaceFirst("^get", ""));
		eAttribute.setName(attributeName);
		markOptional(accessor, eAttribute);
		context.resolve(accessor, getType(accessor), this::buildBasicType, debug("Setting type of attribute " + eAttribute.getName(), eAttribute::setEType));
		return eAttribute;
	}
	
	private <EC extends ENamedElement> Consumer<EC> debug(String tag, Consumer<EC> toDebug) {
		return (value) -> {
			System.out.println(tag + " - " + value);
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
