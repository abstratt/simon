package com.abstratt.simon.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.ecore.util.EcoreValidator;
import org.junit.jupiter.api.Test;

import com.abstratt.simon.compiler.source.ecore.EPackageMetamodelSource;
import com.abstratt.simon.compiler.source.ecore.Java2EcoreMapper;
import com.abstratt.simon.examples.im.IM;
import com.abstratt.simon.examples.ui.UI;
import com.abstratt.simon.examples.ui.UI.Application;
import com.abstratt.simon.examples.ui.UI.PanelLayout;
import com.abstratt.simon.metamodel.ecore.impl.MetaEcoreHelper;

/**
 * Explores building Ecore-based metamodels (remember, Ecore is the meta-metamodel) from annotated Java models.
 */
public class Java2EcoreTest {

	@Test
	void objectType() {
		var result = build(IM.Namespace.class);
		assertEquals(IM.Namespace.class.getSimpleName(), result.getName());
	}

	@Test
	void rootType() {
		assertTrue(MetaEcoreHelper.isRootComposite(UI.Application.class));
		EClass result = build(IM.Namespace.class);
		assertTrue(MetaEcoreHelper.isRootComposite(result));
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
		var attributes = result.getEAttributes();
		var red = find(attributes, a -> a.getName().equals("red"));
		var green = find(attributes, a -> a.getName().equals("green"));
		var blue = find(attributes, a -> a.getName().equals("blue"));
		assertNotNull(red);
		assertNotNull(green);
		assertNotNull(blue);
	}

	@Test
	void recordTypeAttributes() {
		EClass result = build(UI.Color.class);
		assertEquals(UI.Color.class.getSimpleName(), result.getName());
		var attributes = result.getEAttributes();
		var red = find(attributes, a -> a.getName().equals("red"));
		assertNotNull(red);
		var redEType = MetaEcoreHelper.getValueType(red);
		assertSame(EcorePackage.Literals.EINT, redEType);
	}

	@Test
	void primitiveSlot() {
		EPackage result = build(UI.class);
		var application = (EClass) result.getEClassifier(Application.class.getSimpleName());
		var name = application.getEStructuralFeature("name");
		assertNotNull(name.getEType().getEPackage());
		var nameType = MetaEcoreHelper.getValueType(name);
		assertSame(String.class, nameType.getInstanceClass());
	}

	@Test
	void primitive() {
		EClass result = build(UI.Color.class);
		assertEquals(UI.Color.class.getSimpleName(), result.getName());
		var attributes = result.getEAttributes();

		var red = find(attributes, a -> a.getName().equals("red"));
		var attributeType = red.getEType();
		var redType = MetaEcoreHelper.getValueType(red);
		assertSame(int.class, redType.getInstanceClass());
	}

	@Test
	void enumSlot() {
		EClass result = build(UI.Container.class);
		var attributes = result.getEAttributes();
		var layout = find(attributes, a -> a.getName().equals("layout"));
		var layoutType = layout.getEType();
		assertTrue(layoutType instanceof EEnum, () -> layoutType.getClass().getName());
	}

	@Test
	void enumType() {
		EEnum result = build(UI.PanelLayout.class);
		assertEquals(UI.PanelLayout.class.getSimpleName(), result.getName());
		var literals = result.getELiterals();
		UI.PanelLayout[] javaValues = UI.PanelLayout.values();
		for (var panelLayout : javaValues) {
			var found = find(literals, l -> panelLayout.name().equals(l.getLiteral()));
			assertNotNull(found);
		}
	}

	@Test
	void parent() {
		EPackage uiPackage = build(UI.class);
		var containerEClass = (EClass) uiPackage.getEClassifier(name(UI.Container.class));
		var componentEClass = (EClass) uiPackage.getEClassifier(name(UI.Component.class));
		var containerParent = find(containerEClass.getEAllReferences(), e -> e.getName().equals("parent"));
		var containerChildren = find(containerEClass.getEAllReferences(), e -> e.getName().equals("children"));
		var componentParent = find(componentEClass.getEAllReferences(), e -> e.getName().equals("parent"));
		assertSame(containerChildren, componentParent.getEOpposite());
		assertTrue(componentParent.isContainer());
		assertTrue(containerChildren.isContainment());
		assertTrue(containerParent.isContainer());
	}

	@Test
	void superClass() {
		EPackage uiPackage = build(UI.class);
		var componentEClass = (EClass) uiPackage.getEClassifier(name(UI.Component.class));
		var containerEClass = (EClass) uiPackage.getEClassifier(name(UI.Container.class));
		var namedEClass = (EClass) uiPackage.getEClassifier("Named");

		assertTrue(componentEClass.isSuperTypeOf(containerEClass));
		assertTrue(namedEClass.isSuperTypeOf(componentEClass));
		assertTrue(namedEClass.getESuperTypes().isEmpty());
	}

	@Test
	void superClass2() {
		EPackage uiPackage = build(IM.class);
		var namedEClass = (EClass) uiPackage.getEClassifier(name(IM.Named.class));
		var typeEClass = (EClass) uiPackage.getEClassifier(name(IM.Type.class));
		var basicTypeEClass = (EClass) uiPackage.getEClassifier(name(IM.BasicType.class));
		var primitiveEClass = (EClass) uiPackage.getEClassifier(name(IM.Primitive.class));

		assertTrue(namedEClass.getESuperTypes().isEmpty());
		assertTrue(namedEClass.isSuperTypeOf(typeEClass));
		assertTrue(typeEClass.isSuperTypeOf(basicTypeEClass));
		assertTrue(basicTypeEClass.isSuperTypeOf(primitiveEClass));
	}

	@Test
	void packages() {
		EPackage imPackage = build(IM.class);
		assertNotNull(imPackage);
		var allClassifiers = imPackage.getEClassifiers();
		var entityClass = find(allClassifiers, pred((EClassifier eo) -> eo instanceof EClass)//
				.and(pred(eo -> "Entity".equals(eo.getName()))));
		assertNotNull(entityClass);
	}

	@Test
	void containingPackage() {
		EPackage imPackage = build(IM.class);
		assertNotNull(imPackage);
		var entityEClass = imPackage.getEClassifier(name(IM.Entity.class));
		assertNotNull(entityEClass);
		assertSame(imPackage, entityEClass.getEPackage());
	}

	@Test
	void optional() {
		EPackage uiPackage = build(UI.class);
		var componentEClass = (EClass) uiPackage.getEClassifier(name(UI.Component.class));
		var componentParent = find(componentEClass.getEReferences(), e -> e.getName().equals("parent"));
		assertFalse(componentParent.isRequired());
	}

	@Test
	void reflexiveHierarchy() {
		EClass containerEClass = build(UI.Container.class);
		var containerChildren = find(containerEClass.getEAllReferences(),
				pred(EReference::isContainment).and(e -> e.getName().equals("children")));
		assertNotNull(containerChildren.getEOpposite());
		var containerParent = find(containerEClass.getEAllReferences(),
				pred(EReference::isContainer).and(e -> e.getName().equals("parent")));

		assertSame(containerChildren, containerParent.getEOpposite());
	}

	@Test
	void reflexive() {
		EClass containerEClass = build(UI.Container.class);
		var containerChildren = find(containerEClass.getEReferences(), e -> e.getName().equals("children"));
		assertNotNull(containerChildren.getEOpposite());
		var containerParent = find(containerEClass.getEAllReferences(), e -> e.getName().equals("parent"));

		assertSame(containerChildren, containerParent.getEOpposite());
	}

	@Test
	void children() {
		EClass namespaceEClass = build(IM.Namespace.class);
		var entities = find(namespaceEClass.getEAllReferences(),
				pred(EReference::isContainment).and(e -> e.getName().equals("entities")));
		assertNotNull(entities);
		var entityEClass = ((EClass) entities.getEType());
		assertEquals("Entity", entityEClass.getName());
		var properties = find(entityEClass.getEAllReferences(),
				pred(EReference::isContainment).and(e -> e.getName().equals("properties")));
		assertNotNull(properties);
		var propertyEClass = ((EClass) properties.getEType());
		assertEquals("Property", propertyEClass.getName());
	}

	@Test
	void related() {
		EClass linkEClass = build(UI.Link.class);
		var targetScreen = find(linkEClass.getEAllReferences(),
				pred(EReference::isContainment).negate().and(e -> e.getName().equals("targetScreen")));
		assertNotNull(targetScreen);
		var entityEClass = ((EClass) targetScreen.getEType());
		assertEquals("Screen", entityEClass.getName());
	}

	@Test
	void interfaceAttributes() {
		EClass namedEclass = build(UI.Named.class);
		var allAttributes = namedEclass.getEAllAttributes();
		assertFalse(allAttributes.isEmpty());
		var name = find(allAttributes, e -> e.getName().equals("name"));
		assertNotNull(name);
		var nameEType = MetaEcoreHelper.getValueType(name);
		assertSame(EcorePackage.Literals.ESTRING, nameEType);
	}

	@Test
	void abstractClassAttributes() {
		EClass containerEclass = build(UI.Container.class);
		var layout = find(containerEclass.getEAllAttributes(), e -> e.getName().equals("layout"));
		assertNotNull(layout);
		var layoutEType = ((EEnum) layout.getEType());
		assertEquals(PanelLayout.class.getSimpleName(), layoutEType.getName());
	}

	@Test
	void ownAttributes() {
		EClass linkEclass = build(UI.Link.class);
		var label = find(linkEclass.getEAllAttributes(), e -> e.getName().equals("label"));
		assertNotNull(label);
		assertSame(EcorePackage.Literals.ESTRING,
				MetaEcoreHelper.getValueFeature(((EClass) label.getEType())).getEType());
	}

	@Test
	void inheritedAttributes() {
		EClass applicationEclass = build(UI.Application.class);
		var name = find(applicationEclass.getEAllAttributes(), e -> e.getName().equals("name"));
		assertNotNull(name);
		var nameEType = MetaEcoreHelper.getValueType(name);
		assertSame(EcorePackage.Literals.ESTRING, nameEType);
	}
	
	@Test
	void inheritedReferences() {
		EClass relationshipEclass = build(IM.Relationship.class);
		assertNotNull(relationshipEclass.getEPackage());
		var typeReference = find(relationshipEclass.getEAllReferences(), e -> e.getName().equals("type"));
		assertNotNull(typeReference);
		var typeReferenceEClass = typeReference.getEReferenceType();
		assertEquals("Type", typeReferenceEClass.getName());
		assertSame(relationshipEclass.getEPackage(), typeReferenceEClass.getEPackage());
	}

	@Test
	void opposite() {
		EClass entityEClass = build(IM.Entity.class);
		var superTypes = find(entityEClass.getEAllReferences(),
				pred(EReference::isContainment).negate().and(e -> e.getName().equals("superTypes")));
		var subTypes = find(entityEClass.getEAllReferences(),
				pred(EReference::isContainment).negate().and(e -> e.getName().equals("subTypes")));
		assertNotNull(superTypes);
		assertNotNull(subTypes);
		assertSame(subTypes, superTypes.getEOpposite());
	}

	@Test
	void typeResolution() {
		EPackage uiPackage = build(UI.class);
		var typeSource = new EPackageMetamodelSource.Factory(uiPackage).build();
		var resolved = typeSource.resolveType("Application", null);
		assertNotNull(resolved);
	}

	<E extends EObject> List<E> filter(List<E> toFilter, Predicate<E> predicate) {
		return toFilter.stream().filter(predicate).collect(Collectors.toList());
	}

	<E extends EObject> E find(List<E> toFilter, Predicate<E> predicate) {
		return toFilter.stream().filter(predicate).findAny().orElse(null);
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
		var diagContext = Diagnostician.INSTANCE.createDefaultContext();
		var diagnostic = new BasicDiagnostic();
		EcoreValidator.INSTANCE.validate(built, diagnostic, diagContext);
		var severity = diagnostic.getSeverity();
		assertTrue(severity < Diagnostic.ERROR, diagnostic::toString);
	}
}
