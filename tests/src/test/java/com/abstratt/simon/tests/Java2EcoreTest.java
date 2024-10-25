package com.abstratt.simon.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.abstratt.simon.compiler.source.ecore.EPackageMetamodelSource;
import com.abstratt.simon.compiler.source.ecore.Java2EcoreMapper;
import com.abstratt.simon.examples.JavaExampleLanguages;
import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.TreeIterator;
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
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import com.abstratt.simon.metamodel.ecore.impl.MetaEcoreHelper;

/**
 * Explores building Ecore-based metamodels (remember, Ecore is the
 * meta-metamodel) from annotated Java models.
 */
public class Java2EcoreTest {

    class ClassConstants {
        public Class<?> IM_BASIC_TYPE_CLASS = getClass("IM.BasicType");
        public Class<?> IM_ENTITY_CLASS = getClass("IM.Entity");
        public Class<?> IM_NAMED_CLASS = getClass("IM.Named");
        public Class<?> IM_NAMESPACE_CLASS = getClass("IM.Namespace");
        public Class<?> IM_PRIMITIVE_CLASS = getClass("IM.Primitive");
        public Class<?> RELATIONSHIP_CLASS = getClass("IM.Relationship");
        public Class<?> IM_TYPE_CLASS = getClass("IM.Type");
        public Class<?> IM_CLASS = getClass("IM");
        public Class<?> UI_APPLICATION_CLASS = getClass("UI.Application");
        public Class<?> UI_BUTTON_CLASS = getClass("UI.Button");
        public Class<?> UI_COLOR_CLASS = getClass("UI.Color");
        public Class<?> UI_COMPONENT_CLASS = getClass("UI.Component");
        public Class<?> UI_CONTAINER_CLASS = getClass("UI.Container");
        public Class<?> UI_LINK_CLASS = getClass("UI.Link");
        public Class<?> UI_NAMED_CLASS = getClass("UI.Named");
        public Class<?> UI_PANEL_LAYOUT_CLASS = getClass("UI.PanelLayout");
        public Class<?> UI_SCREEN_CLASS = getClass("UI.Screen");
        public Class<?> UI2_FORM_CLASS = getClass("UI2.Form");
        public Class<?> UI2_CLASS = getClass("UI2");
        public Class<?> UI3_PROTOTYPE_CLASS = getClass("UI3.IPrototype");
        public Class<?> UI3_CLASS = getClass("UI3");
        public Class<?> UI_CLASS = getClass("UI");
        protected Class<?> getClass(String name) {
            try {
                String packageName = getLanguagePackage();
                String javaClassName = packageName + "." + name.replace(".", "$");
                Class<?> aClass = Class.forName(javaClassName);
                System.out.println("Class for " + name + ": " + aClass);
                return aClass;
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        protected String getLanguagePackage() {
            return JavaExampleLanguages.class.getPackageName();
        }
    }

    private ClassConstants classConstants = getClassConstants();

    @NotNull
    protected ClassConstants getClassConstants() {
        return new ClassConstants();
    }

    @Test
    void objectType() {
        var result = build(classConstants.IM_NAMESPACE_CLASS);
        assertEquals(classConstants.IM_NAMESPACE_CLASS.getSimpleName(), result.getName());
    }

    @Test
    void objectTypeFromAccessor() throws Exception {
        var type = MetaEcoreHelper.getType(classConstants.UI_SCREEN_CLASS.getMethod("application"));
        assertEquals(classConstants.UI_APPLICATION_CLASS, type);
    }

    @Test
    void isObjectType() throws Exception {
        var type = classConstants.UI_APPLICATION_CLASS;
        var annotations = MetaEcoreHelper.getTypeAnnotations(type).collect(Collectors.toList());
        assertEquals(Collections.emptyList(), annotations);
        assertFalse(MetaEcoreHelper.isExplicitlyObjectType(type));
        assertTrue(MetaEcoreHelper.isImplicitlyObjectType(type));
        assertTrue(MetaEcoreHelper.isObjectType(type));
    }

    @Test
    void rootType() {
        assertTrue(MetaEcoreHelper.isRootComposite(classConstants.UI_APPLICATION_CLASS));
        EClass result = build(classConstants.IM_NAMESPACE_CLASS);
        assertTrue(MetaEcoreHelper.isRootComposite(result));
    }

    @Test
    void abstractClass() {
        EClass component = build(classConstants.UI_COMPONENT_CLASS);
        EClass button = build(classConstants.UI_BUTTON_CLASS);
        assertTrue(!button.isAbstract());
        assertTrue(component.isAbstract());
    }

    @Test
    void instantiable() {
        EClass named = build(classConstants.UI_NAMED_CLASS);
        assertTrue(named.isAbstract());
        assertFalse(named.isInterface());
    }

    @Test
    void recordType() {
        EClass result = build(classConstants.UI_COLOR_CLASS);
        assertEquals(classConstants.UI_COLOR_CLASS.getSimpleName(), result.getName());
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
        EClass result = build(classConstants.UI_COLOR_CLASS);
        assertEquals(classConstants.UI_COLOR_CLASS.getSimpleName(), result.getName());
        var attributes = result.getEAttributes();
        var red = find(attributes, a -> a.getName().equals("red"));
        assertNotNull(red);
        var redEType = MetaEcoreHelper.getValueType(red);
        assertSame(EcorePackage.Literals.EINT, redEType);
    }

    @Test
    void primitiveSlot() {
        EPackage result = build(classConstants.UI_CLASS);
        var application = (EClass) result.getEClassifier(classConstants.UI_APPLICATION_CLASS.getSimpleName());
        var name = application.getEStructuralFeature("name");
        assertNotNull(name.getEType().getEPackage());
        var nameType = MetaEcoreHelper.getValueType(name);
        assertSame(String.class, nameType.getInstanceClass());
    }

    @Test
    void primitive() {
        EClass result = build(classConstants.UI_COLOR_CLASS);
        assertEquals(classConstants.UI_COLOR_CLASS.getSimpleName(), result.getName());
        var attributes = result.getEAttributes();

        var red = find(attributes, a -> a.getName().equals("red"));
        var attributeType = red.getEType();
        var redType = MetaEcoreHelper.getValueType(red);
        assertSame(int.class, redType.getInstanceClass());
    }

    @Test
    void enumSlot() {
        EClass result = build(classConstants.UI_CONTAINER_CLASS);
        var attributes = result.getEAttributes();
        var layout = find(attributes, a -> a.getName().equals("layout"));
        var layoutType = layout.getEType();
        assertTrue(layoutType instanceof EEnum, () -> layoutType.getClass().getName());
    }

    @Test
    void enumType() {
        EEnum result = build(classConstants.UI_PANEL_LAYOUT_CLASS);
        assertEquals(classConstants.UI_PANEL_LAYOUT_CLASS.getSimpleName(), result.getName());
        var literals = result.getELiterals();
        for (var panelLayout : classConstants.UI_PANEL_LAYOUT_CLASS.getEnumConstants()) {
            var found = find(literals, l -> ((Enum) panelLayout).name().equals(l.getLiteral()));
            assertNotNull(found);
        }
    }

    @Test
    void parent() {
        EPackage uiPackage = build(classConstants.UI_CLASS);
        var containerEClass = (EClass) uiPackage.getEClassifier(name(classConstants.UI_CONTAINER_CLASS));
        var componentEClass = (EClass) uiPackage.getEClassifier(name(classConstants.UI_COMPONENT_CLASS));
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
        EPackage uiPackage = build(classConstants.UI_CLASS);
        var componentEClass = (EClass) uiPackage.getEClassifier(name(classConstants.UI_COMPONENT_CLASS));
        var containerEClass = (EClass) uiPackage.getEClassifier(name(classConstants.UI_CONTAINER_CLASS));
        var namedEClass = (EClass) uiPackage.getEClassifier("Named");

        assertTrue(componentEClass.isSuperTypeOf(containerEClass));
        assertTrue(namedEClass.isSuperTypeOf(componentEClass));
        assertTrue(namedEClass.getESuperTypes().isEmpty());

        var nameAttributes = filter(containerEClass.getEAllStructuralFeatures(), e -> e.getName().equals("name"));
        assertEquals(1, nameAttributes.size());
    }

    @Test
    void superClass2() {
        EPackage uiPackage = build(classConstants.IM_CLASS);
        var namedEClass = (EClass) uiPackage.getEClassifier(name(classConstants.IM_NAMED_CLASS));
        var typeEClass = (EClass) uiPackage.getEClassifier(name(classConstants.IM_TYPE_CLASS));
        var basicTypeEClass = (EClass) uiPackage.getEClassifier(name(classConstants.IM_BASIC_TYPE_CLASS));
        var primitiveEClass = (EClass) uiPackage.getEClassifier(name(classConstants.IM_PRIMITIVE_CLASS));

        assertTrue(namedEClass.getESuperTypes().isEmpty());
        assertTrue(namedEClass.isSuperTypeOf(typeEClass));
        assertTrue(typeEClass.isSuperTypeOf(basicTypeEClass));
        assertTrue(basicTypeEClass.isSuperTypeOf(primitiveEClass));
    }

    @Test
    void packages() {
        EPackage imPackage = build(classConstants.IM_CLASS);
        assertNotNull(imPackage);
        var allClassifiers = imPackage.getEClassifiers();
        var entityClass = find(allClassifiers, pred((EClassifier eo) -> eo instanceof EClass)//
                .and(pred(eo -> "Entity".equals(eo.getName()))));
        assertNotNull(entityClass);
    }

    @Test
    void containingPackage() {
        EPackage imPackage = build(classConstants.IM_CLASS);
        assertNotNull(imPackage);
        var entityEClass = imPackage.getEClassifier(name(classConstants.IM_ENTITY_CLASS));
        assertNotNull(entityEClass);
        assertSame(imPackage, entityEClass.getEPackage());
    }

    @Test
    void optional() {
        EPackage uiPackage = build(classConstants.UI_CLASS);
        var componentEClass = (EClass) uiPackage.getEClassifier(name(classConstants.UI_COMPONENT_CLASS));
        var componentParent = find(componentEClass.getEAllReferences(), e -> e.getName().equals("parent"));
        assertFalse(componentParent.isRequired());
    }

    @Test
    void reflexiveHierarchy() {
        EClass containerEClass = build(classConstants.UI_CONTAINER_CLASS);
        var containerChildren = find(containerEClass.getEAllReferences(),
                pred(EReference::isContainment).and(e -> e.getName().equals("children")));
        assertNotNull(containerChildren.getEOpposite());
        var containerParent = find(containerEClass.getEAllReferences(),
                pred(EReference::isContainer).and(e -> e.getName().equals("parent")));

        assertSame(containerChildren, containerParent.getEOpposite());
    }

    @Test
    void reflexive() {
        EClass containerEClass = build(classConstants.UI_CONTAINER_CLASS);
        var containerChildren = find(containerEClass.getEReferences(), e -> e.getName().equals("children"));
        assertNotNull(containerChildren.getEOpposite());
        var containerParent = find(containerEClass.getEAllReferences(), e -> e.getName().equals("parent"));

        assertSame(containerChildren, containerParent.getEOpposite());
    }

    @Test
    void children() {
        EClass namespaceEClass = build(classConstants.IM_NAMESPACE_CLASS);
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
        EClass linkEClass = build(classConstants.UI_LINK_CLASS);
        var targetScreen = find(linkEClass.getEAllReferences(),
                pred(EReference::isContainment).negate().and(e -> e.getName().equals("targetScreen")));
        assertNotNull(targetScreen);
        var entityEClass = ((EClass) targetScreen.getEType());
        assertEquals("Screen", entityEClass.getName());
    }

    @Test
    void interfaceAttributes() {
        EClass namedEclass = build(classConstants.UI_NAMED_CLASS);
        var allAttributes = namedEclass.getEAllAttributes();
        assertFalse(allAttributes.isEmpty());
        var name = find(allAttributes, e -> e.getName().equals("name"));
        assertNotNull(name);
        var nameEType = MetaEcoreHelper.getValueType(name);
        assertSame(EcorePackage.Literals.ESTRING, nameEType);
    }

    @Test
    void abstractClassAttributes() {
        EClass containerEclass = build(classConstants.UI_CONTAINER_CLASS);
        var layout = find(containerEclass.getEAllAttributes(), e -> e.getName().equals("layout"));
        assertNotNull(layout);
        var layoutEType = ((EEnum) layout.getEType());
        assertEquals(classConstants.UI_PANEL_LAYOUT_CLASS.getSimpleName(), layoutEType.getName());
    }

    @Test
    void ownAttributes() {
        EClass linkEclass = build(classConstants.UI_LINK_CLASS);
        var label = find(linkEclass.getEAllAttributes(), e -> e.getName().equals("label"));
        assertNotNull(label);
        assertSame(EcorePackage.Literals.ESTRING,
                MetaEcoreHelper.getValueFeature(((EClass) label.getEType())).getEType());
    }

    @Test
    void inheritedAttributes() {
        EClass applicationEclass = build(classConstants.UI_APPLICATION_CLASS);
        var name = find(applicationEclass.getEAllAttributes(), e -> e.getName().equals("name"));
        assertNotNull(name);
        var nameEType = MetaEcoreHelper.getValueType(name);
        assertSame(EcorePackage.Literals.ESTRING, nameEType);
    }

    @Test
    void inheritedReferences() {
        EClass relationshipEclass = build(classConstants.RELATIONSHIP_CLASS);
        assertNotNull(relationshipEclass.getEPackage());
        var typeReference = find(relationshipEclass.getEAllReferences(), e -> e.getName().equals("type"));
        assertNotNull(typeReference);
        var typeReferenceEClass = typeReference.getEReferenceType();
        assertEquals("Type", typeReferenceEClass.getName());
        assertSame(relationshipEclass.getEPackage(), typeReferenceEClass.getEPackage());
    }

    @Test
    void dependentPackageViaInheritance() {
        EPackage ui2Package = build(classConstants.UI2_CLASS);

        assertNotNull(ui2Package);
        assertEquals("UI2", ui2Package.getName());
        var formClass = (EClass) ui2Package.getEClassifier(classConstants.UI2_FORM_CLASS.getSimpleName());
        assertNotNull(formClass);

        var superTypes = formClass.getESuperTypes();
        assertEquals(1, superTypes.size());
        var componentClass = superTypes.get(0);
        assertEquals(classConstants.UI_CONTAINER_CLASS.getSimpleName(), componentClass.getName());
        assertNotNull(componentClass.getEPackage());
        assertEquals("UI", componentClass.getEPackage().getName());
        var uiPackage = componentClass.getEPackage();

        var namedClass = find(componentClass.getEAllSuperTypes(), cls -> cls.getName().equals("Named"));
        assertNotNull(namedClass);
        var formName = formClass.getEStructuralFeature("name");
        var componentName = componentClass.getEStructuralFeature("name");
        var namedName = namedClass.getEStructuralFeature("name");

        assertNotNull(formName);
        assertNotNull(componentName);
        assertSame(componentName, formName);

        assertNotNull(namedName);
        assertSame(componentName, namedName);

        var nameAttributes = filter(formClass.getEAllStructuralFeatures(), e -> e.getName().equals("name"));
        assertEquals(1, nameAttributes.size());

        assertNotNull(uiPackage.eResource());
        assertNotNull(ui2Package.eResource());
        assertSame(uiPackage.eResource(), ui2Package.eResource());
        assertEquals(2, ui2Package.eResource().getContents().size());

        assertEquals(1, filter(uiPackage.eResource().getAllContents(),
                e -> e instanceof EClass && ((EClass) e).getName().equals("Named")).size());
    }

    @Test
    void dependentPackageViaReference() {
        EPackage ui3Package = build(classConstants.UI3_CLASS);
        assertNotNull(ui3Package);
        assertEquals("UI3", ui3Package.getName());
        var prototypeClass = (EClass) ui3Package.getEClassifier(classConstants.UI3_PROTOTYPE_CLASS.getSimpleName());
        assertNotNull(prototypeClass);
        var applicationsRef = find(prototypeClass.getEAllReferences(), ref -> ref.getName().equals("applications"));
        var applicationEClass = applicationsRef.getEReferenceType();
        assertNotNull(applicationEClass);
        assertNotNull(applicationEClass.getEPackage());
        assertEquals("UI", applicationEClass.getEPackage().getName());
    }

    @Test
    void opposite() {
        EClass entityEClass = build(classConstants.IM_ENTITY_CLASS);
        var superTypes = find(entityEClass.getEAllReferences(),
                pred(EReference::isContainment).negate().and(e -> e.getName().equals("superTypes")));
        var subTypes = find(entityEClass.getEAllReferences(),
                pred(EReference::isContainment).negate().and(e -> e.getName().equals("subTypes")));
        assertNotNull(superTypes);
        assertNotNull(subTypes);
        assertSame(subTypes, superTypes.getEOpposite());
    }
    
    @Test
    void escaping() {
        EClass entityEClass = build(classConstants.IM_ENTITY_CLASS);
        var abstract_ = entityEClass.getEStructuralFeature("abstract");
        assertNotNull(abstract_);
        assertEquals("abstract", abstract_.getName());
    }

    @Test
    void typeDeclaration() {
        EClass applicationClass = build(classConstants.UI_APPLICATION_CLASS);
        var screens = applicationClass.getEStructuralFeature("screens");
        assertNotNull(screens.getEType().getEPackage());
        assertTrue(screens.isMany());
        assertEquals(classConstants.UI_SCREEN_CLASS.getSimpleName(), screens.getEType().getName());
    }

    @Test
    void typeResolution() {
        EPackage uiPackage = build(classConstants.UI_CLASS);
        var typeSource = new EPackageMetamodelSource.Factory(uiPackage).build();
        var resolved = typeSource.resolveType("Application", null);
        assertNotNull(resolved);
    }

    <E extends EObject> List<E> filter(List<E> toFilter, Predicate<E> predicate) {
        return toFilter.stream().filter(predicate).collect(Collectors.toList());
    }

    <E extends EObject> List<E> filter(TreeIterator<E> toFilter, Predicate<E> predicate) {
        var result = new ArrayList<E>();
        toFilter.forEachRemaining(it -> {
            if (predicate.test(it))
                result.add(it);
        });
        return result;
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
