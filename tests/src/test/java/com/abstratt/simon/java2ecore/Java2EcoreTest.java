package com.abstratt.simon.java2ecore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.DiagnosticChain;
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
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.ecore.util.EcoreValidator;
import org.junit.jupiter.api.Test;

import com.abstratt.simon.examples.Kirra;
import com.abstratt.simon.examples.UI;
import com.abstratt.simon.examples.UI.PanelLayout;

public class Java2EcoreTest {
	
	@Test
	void objectType() {
		EClass result = build(Kirra.Namespace.class);
		assertEquals(Kirra.Namespace.class.getSimpleName(), result.getName());
	}

	@Test
	void abstractClass() {
		EClass component = build(UI.Component.class);
		EClass button = build(UI.Button.class);
		assertTrue(!button.isAbstract());
		assertTrue(component.isAbstract());
	}

	@Test
	void interfaces() {
		EClass named = build(UI.Named.class);
		assertTrue(named.isAbstract());
		assertTrue(named.isInterface());
	}

	@Test
	void recordType() {
		EClass result = build(UI.Color.class);
		assertEquals(UI.Color.class.getSimpleName(), result.getName());
		EList<EAttribute> attributes = result.getEAttributes();
		assertNotNull(find(attributes, a -> a.getName().equals("red")));
		assertNotNull(find(attributes, a -> a.getName().equals("green")));
		assertNotNull(find(attributes, a -> a.getName().equals("blue")));
	}

	@Test
	void primitive() {
		EClass result = build(UI.Color.class);
		assertEquals(UI.Color.class.getSimpleName(), result.getName());
		EList<EAttribute> attributes = result.getEAttributes();

		EAttribute red = find(attributes, a -> a.getName().equals("red"));
		EClassifier redType = red.getEType();
		assertSame(int.class, redType.getInstanceClass());
	}
	
	@Test
	void enumSlot() {
		EClass result = build(UI.Container.class);
		EList<EAttribute> attributes = result.getEAttributes();
		EAttribute layout = find(attributes, a -> a.getName().equals("layout"));
		EClassifier layoutType = layout.getEType();
		assertTrue(layoutType instanceof EEnum, () -> layoutType.getClass().getName());
	}

	@Test
	void enumType() {
		EEnum result = build(UI.PanelLayout.class);
		assertEquals(UI.PanelLayout.class.getSimpleName(), result.getName());
		EList<EEnumLiteral> literals = result.getELiterals();
		UI.PanelLayout[] javaValues = UI.PanelLayout.values();
		for (UI.PanelLayout panelLayout : javaValues) {
			EEnumLiteral found = find(literals, l -> panelLayout.name().equals(l.getLiteral()));
			assertNotNull(found);
		}
	}

	@Test
	void parent() {
		EPackage uiPackage = build(UI.class);
		EClass containerEClass = (EClass) uiPackage.getEClassifier(name(UI.Container.class));
		EClass componentEClass = (EClass) uiPackage.getEClassifier(name(UI.Component.class));
		EReference containerParent = find(containerEClass.getEAllReferences(), e -> e.getName().equals("parent"));
		EReference containerChildren = find(containerEClass.getEAllReferences(), e -> e.getName().equals("children"));
		EReference componentParent = find(componentEClass.getEAllReferences(), e -> e.getName().equals("parent"));
		assertSame(containerChildren, componentParent.getEOpposite());
		assertTrue(componentParent.isContainer());
		assertTrue(containerChildren.isContainment());
		assertTrue(containerParent.isContainer());
	}

	@Test
	void superClass() {
		EPackage uiPackage = build(UI.class);
		EClass componentEClass = (EClass) uiPackage.getEClassifier(name(UI.Component.class));
		EClass containerEClass = (EClass) uiPackage.getEClassifier(name(UI.Container.class));
		EClass namedEClass = (EClass) uiPackage.getEClassifier("Named");

		assertTrue(componentEClass.isSuperTypeOf(containerEClass));
		assertTrue(namedEClass.isSuperTypeOf(componentEClass));
		assertTrue(namedEClass.getESuperTypes().isEmpty());
	}

	@Test
	void packages() {
		EPackage kirraPackage = build(Kirra.class);
		assertNotNull(kirraPackage);
		EList<EClassifier> allClassifiers = kirraPackage.getEClassifiers();
		Object entityClass = find(allClassifiers, pred((EClassifier eo) -> eo instanceof EClass)//
				.and(pred(eo -> "Entity".equals(eo.getName()))));
		assertNotNull(entityClass);
	}

	@Test
	void optional() {
		EPackage uiPackage = build(UI.class);
		EClass componentEClass = (EClass) uiPackage.getEClassifier(name(UI.Component.class));
		EReference componentParent = find(componentEClass.getEReferences(), e -> e.getName().equals("parent"));
		assertFalse(componentParent.isRequired());
	}

	@Test
	void reflexiveHierarchy() {
		EClass containerEClass = build(UI.Container.class);
		EReference containerChildren = find(containerEClass.getEAllReferences(),
				pred(EReference::isContainment).and(e -> e.getName().equals("children")));
		assertNotNull(containerChildren.getEOpposite());
		EReference containerParent = find(containerEClass.getEAllReferences(),
				pred(EReference::isContainer).and(e -> e.getName().equals("parent")));

		assertSame(containerChildren, containerParent.getEOpposite());
	}

	@Test
	void reflexive() {
		EClass containerEClass = build(UI.Container.class);
		EReference containerChildren = find(containerEClass.getEReferences(), e -> e.getName().equals("children"));
		assertNotNull(containerChildren.getEOpposite());
		EReference containerParent = find(containerEClass.getEAllReferences(), e -> e.getName().equals("parent"));

		assertSame(containerChildren, containerParent.getEOpposite());
	}

	@Test
	void children() {
		EClass namespaceEClass = build(Kirra.Namespace.class);
		EReference entities = find(namespaceEClass.getEAllReferences(),
				pred(EReference::isContainment).and(e -> e.getName().equals("entities")));
		assertNotNull(entities);
		EClass entityEClass = ((EClass) entities.getEType());
		assertEquals("Entity", entityEClass.getName());
		EReference properties = find(entityEClass.getEAllReferences(),
				pred(EReference::isContainment).and(e -> e.getName().equals("properties")));
		assertNotNull(properties);
		EClass propertyEClass = ((EClass) properties.getEType());
		assertEquals("Property", propertyEClass.getName());
	}

	@Test
	void related() {
		EClass linkEClass = build(UI.Link.class);
		EReference targetScreen = find(linkEClass.getEAllReferences(),
				pred(EReference::isContainment).negate().and(e -> e.getName().equals("targetScreen")));
		assertNotNull(targetScreen);
		EClass entityEClass = ((EClass) targetScreen.getEType());
		assertEquals("Screen", entityEClass.getName());
	}

	@Test
	void interfaceAttributes() {
		EClass namedEclass = build(UI.Named.class);
		EList<EAttribute> allAttributes = namedEclass.getEAllAttributes();
		assertFalse(allAttributes.isEmpty());
		EAttribute name = find(allAttributes, e -> e.getName().equals("name"));
		assertNotNull(name);
		EDataType nameEType = ((EDataType) name.getEType());
		assertSame(EcorePackage.Literals.ESTRING, nameEType);
	}

	@Test
	void abstractClassAttributes() {
		EClass containerEclass = build(UI.Container.class);
		EAttribute layout = find(containerEclass.getEAllAttributes(), e -> e.getName().equals("layout"));
		assertNotNull(layout);
		EEnum layoutEType = ((EEnum) layout.getEType());
		assertEquals(PanelLayout.class.getSimpleName(), layoutEType.getName());
	}

	@Test
	void ownAttributes() {
		EClass linkEclass = build(UI.Link.class);
		EAttribute label = find(linkEclass.getEAllAttributes(), e -> e.getName().equals("label"));
		assertNotNull(label);
		EDataType labelEType = ((EDataType) label.getEType());
		assertSame(EcorePackage.Literals.ESTRING, labelEType);
	}

	@Test
	void inheritedAttributes() {
		EClass applicationEclass = build(UI.Application.class);
		EAttribute name = find(applicationEclass.getEAllAttributes(), e -> e.getName().equals("name"));
		assertNotNull(name);
		EDataType nameEType = ((EDataType) name.getEType());
		assertSame(EcorePackage.Literals.ESTRING, nameEType);
	}

	@Test
	void opposite() {
		EClass entityEClass = build(Kirra.Entity.class);
		EReference superTypes = find(entityEClass.getEAllReferences(),
				pred(EReference::isContainment).negate().and(e -> e.getName().equals("superTypes")));
		EReference subTypes = find(entityEClass.getEAllReferences(),
				pred(EReference::isContainment).negate().and(e -> e.getName().equals("subTypes")));
		assertNotNull(superTypes);
		assertNotNull(subTypes);
		assertSame(subTypes, superTypes.getEOpposite());
	}

	<E extends EObject> List<E> filter(List<E> toFilter, Predicate<E> predicate) {
		return toFilter.stream().filter(predicate).collect(Collectors.toList());
	}

	<E extends EObject> E find(List<E> toFilter, Predicate<E> predicate) {
		return toFilter.stream().filter(predicate).findAny().get();
	}

	<E extends EObject> Predicate<E> pred(Predicate<E> p1) {
		return p1;
	}

	<E extends EObject> Predicate<E> compose(Predicate<E> p1, Predicate<E> p2) {
		return p1.and(p2);
	}

	private String name(Class<?> clazz) {
		return clazz.getSimpleName();
	}

	private <E extends ENamedElement> E build(Class<?> clazz) {
		E built = new Java2EcoreMapper().map(clazz);
		ensureValid(built);
		return built;
	}

	private <E extends ENamedElement> void ensureValid(E built) {
		Diagnostician diagnostics = Diagnostician.INSTANCE;
		Map<Object, Object> diagContext = diagnostics.createDefaultContext();
		DiagnosticChain diagnostic = diagnostics.createDefaultDiagnostic(built);
		boolean validation = EcoreValidator.INSTANCE.validate(built, diagnostic, diagContext);
		assertTrue(validation, diagnostic::toString);
	}
}
