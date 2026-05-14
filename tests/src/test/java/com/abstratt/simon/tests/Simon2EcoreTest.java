package com.abstratt.simon.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.ecore.util.EcoreValidator;
import org.junit.jupiter.api.Test;

import com.abstratt.simon.compiler.source.ClasspathSourceProvider;
import com.abstratt.simon.compiler.source.java.Java2EcoreMapper;
import com.abstratt.simon.compiler.source.simon.SimonFileMetamodelSource;
import com.abstratt.simon.examples.DAUI;
import com.abstratt.simon.examples.IM;
import com.abstratt.simon.examples.Simon;
import com.abstratt.simon.examples.UI;
import com.abstratt.simon.examples.UI2;
import com.abstratt.simon.examples.UI3;
import com.abstratt.simon.metamodel.ecore.impl.MetaEcoreHelper;

/**
 * Parity tests: every example metamodel re-expressed in {@code @language Simon}
 * must produce an EPackage observationally equivalent to its hand-written
 * Java counterpart.
 */
public class Simon2EcoreTest {

    private static final Map<String, EPackage> SIMON_PACKAGES = loadSimonPackages();

    private static Map<String, EPackage> loadSimonPackages() {
        var sources = new ClasspathSourceProvider(UI.class.getClassLoader(),
                "com/abstratt/simon/examples", "simon");
        var factory = SimonFileMetamodelSource.Factory.withBootstrapClass(
                Simon.class,
                Arrays.asList("ui", "ui2", "ui3", "im-metamodel", "daui"),
                sources);
        try (var source = factory.build()) {
            var simonSource = (SimonFileMetamodelSource) source;
            return new LinkedHashMap<>(simonSource.getPackages());
        }
    }

    private EPackage uiPackage() { return ensureValid(SIMON_PACKAGES.get("UI")); }
    private EPackage ui2Package() { return ensureValid(SIMON_PACKAGES.get("UI2")); }
    private EPackage ui3Package() { return ensureValid(SIMON_PACKAGES.get("UI3")); }
    private EPackage imPackage() { return ensureValid(SIMON_PACKAGES.get("IM")); }
    private EPackage dauiPackage() { return ensureValid(SIMON_PACKAGES.get("DAUI")); }

    @Test
    void packagesLoad() {
        assertNotNull(uiPackage());
        assertNotNull(ui2Package());
        assertNotNull(ui3Package());
        assertNotNull(imPackage());
        assertNotNull(dauiPackage());
    }

    @Test
    void rootComposite() {
        assertTrue(MetaEcoreHelper.isRootComposite(eClassIn(uiPackage(), "Application")));
        assertTrue(MetaEcoreHelper.isRootComposite(eClassIn(imPackage(), "Namespace")));
    }

    @Test
    void abstractClasses() {
        assertTrue(eClassIn(uiPackage(), "Named").isAbstract());
        assertTrue(eClassIn(uiPackage(), "Component").isAbstract());
        assertFalse(eClassIn(uiPackage(), "Button").isAbstract());
    }

    @Test
    void recordType() {
        EClass color = eClassIn(uiPackage(), "Color");
        assertTrue(MetaEcoreHelper.isRecord(color));
        assertNotNull(findFeature(color, "red"));
        assertNotNull(findFeature(color, "green"));
        assertNotNull(findFeature(color, "blue"));
    }

    @Test
    void recordTypeAttributePrimitive() {
        EClass color = eClassIn(uiPackage(), "Color");
        EAttribute red = (EAttribute) findFeature(color, "red");
        var redValueType = MetaEcoreHelper.getValueType(red);
        assertSame(EcorePackage.Literals.EINT, redValueType);
    }

    @Test
    void enumType() {
        EClassifier panelLayout = uiPackage().getEClassifier("PanelLayout");
        assertTrue(panelLayout instanceof EEnum);
        EEnum eEnum = (EEnum) panelLayout;
        assertNotNull(find(eEnum.getELiterals(), l -> "Vertical".equals(l.getName())));
        assertNotNull(find(eEnum.getELiterals(), l -> "Horizontal".equals(l.getName())));
    }

    @Test
    void enumSlot() {
        EClass container = eClassIn(uiPackage(), "Container");
        EAttribute layout = (EAttribute) findFeature(container, "layout");
        assertTrue(layout.getEType() instanceof EEnum);
        assertEquals("PanelLayout", layout.getEType().getName());
    }

    @Test
    void parent() {
        EClass container = eClassIn(uiPackage(), "Container");
        EClass component = eClassIn(uiPackage(), "Component");
        EReference children = (EReference) findFeature(container, "children");
        EReference componentParent = (EReference) findFeature(component, "parent");
        assertSame(children, componentParent.getEOpposite());
        assertTrue(children.isContainment());
        assertTrue(componentParent.isContainer());
    }

    @Test
    void superClass() {
        EClass component = eClassIn(uiPackage(), "Component");
        EClass container = eClassIn(uiPackage(), "Container");
        EClass named = eClassIn(uiPackage(), "Named");
        assertTrue(component.isSuperTypeOf(container));
        assertTrue(named.isSuperTypeOf(component));
        assertTrue(named.getESuperTypes().isEmpty());
    }

    @Test
    void imSuperClassChain() {
        EClass named = eClassIn(imPackage(), "Named");
        EClass type = eClassIn(imPackage(), "Type");
        EClass basicType = eClassIn(imPackage(), "BasicType");
        EClass primitive = eClassIn(imPackage(), "Primitive");
        assertTrue(named.getESuperTypes().isEmpty());
        assertTrue(named.isSuperTypeOf(type));
        assertTrue(type.isSuperTypeOf(basicType));
        assertTrue(basicType.isSuperTypeOf(primitive));
    }

    @Test
    void opposite() {
        EClass entity = eClassIn(imPackage(), "Entity");
        EReference superTypes = (EReference) findFeature(entity, "superTypes");
        EReference subTypes = (EReference) findFeature(entity, "subTypes");
        assertNotNull(superTypes);
        assertNotNull(subTypes);
        assertSame(subTypes, superTypes.getEOpposite());
    }

    @Test
    void escapingTrailingUnderscore() {
        EClass entity = eClassIn(imPackage(), "Entity");
        EAttribute abstract_ = (EAttribute) findFeature(entity, "abstract");
        assertNotNull(abstract_);
        assertTrue(MetaEcoreHelper.isModifier(abstract_));
    }

    @Test
    void crossPackageInheritance() {
        EClass form = eClassIn(ui2Package(), "Form");
        assertEquals(1, form.getESuperTypes().size());
        EClass formSuper = form.getESuperTypes().get(0);
        assertEquals("Container", formSuper.getName());
        assertEquals("UI", formSuper.getEPackage().getName());
    }

    @Test
    void crossPackageReference() {
        EClass prototype = eClassIn(ui3Package(), "IPrototype");
        EReference applications = (EReference) findFeature(prototype, "applications");
        assertNotNull(applications);
        assertEquals("Application", applications.getEReferenceType().getName());
        assertEquals("UI", applications.getEReferenceType().getEPackage().getName());
        assertTrue(applications.isMany());
    }

    @Test
    void dauiMultiSuperType() {
        EClass entityScreen = eClassIn(dauiPackage(), "EntityScreen");
        var superNames = entityScreen.getESuperTypes().stream()
                .map(EClass::getName).toList();
        assertTrue(superNames.contains("Screen"));
        assertTrue(superNames.contains("IEntityComponent"));
    }

    @Test
    void modifierAttribute() {
        EClass entity = eClassIn(imPackage(), "Entity");
        EAttribute abstract_ = (EAttribute) findFeature(entity, "abstract");
        assertTrue(MetaEcoreHelper.isModifier(abstract_));
    }

    // --- parity with Java metamodels ---

    @Test
    void uiParity() {
        EPackage javaUI = new Java2EcoreMapper().map(UI.class);
        assertPackagesEquivalent(javaUI, uiPackage());
    }

    @Test
    void imParity() {
        EPackage javaIM = new Java2EcoreMapper().map(IM.class);
        assertPackagesEquivalent(javaIM, imPackage());
    }

    @Test
    void ui2Parity() {
        EPackage javaUI2 = new Java2EcoreMapper().map(UI2.class);
        assertPackagesEquivalent(javaUI2, ui2Package());
    }

    @Test
    void ui3Parity() {
        EPackage javaUI3 = new Java2EcoreMapper().map(UI3.class);
        assertPackagesEquivalent(javaUI3, ui3Package());
    }

    @Test
    void dauiParity() {
        EPackage javaDAUI = new Java2EcoreMapper().map(DAUI.class);
        assertPackagesEquivalent(javaDAUI, dauiPackage());
    }

    // --- helpers ---

    private static void assertPackagesEquivalent(EPackage javaSide, EPackage simonSide) {
        assertEquals(javaSide.getName(), simonSide.getName(), "package name");
        // Compare classifier names (subset: Java side may pull in primitive
        // wrapper classes the Simon side names differently; require Java's
        // EClasses are present on the Simon side).
        for (EClassifier javaClassifier : javaSide.getEClassifiers()) {
            if (!(javaClassifier instanceof EClass) && !(javaClassifier instanceof EEnum)) continue;
            String name = javaClassifier.getName();
            EClassifier simonClassifier = simonSide.getEClassifier(name);
            assertNotNull(simonClassifier, () -> "Missing classifier " + name + " in Simon " + simonSide.getName());
            if (javaClassifier instanceof EClass) {
                assertClassesEquivalent((EClass) javaClassifier, (EClass) simonClassifier);
            }
        }
    }

    private static void assertClassesEquivalent(EClass javaClass, EClass simonClass) {
        assertEquals(javaClass.isAbstract(), simonClass.isAbstract(),
                () -> javaClass.getName() + " abstract");
        assertEquals(MetaEcoreHelper.isRootComposite(javaClass), MetaEcoreHelper.isRootComposite(simonClass),
                () -> javaClass.getName() + " rootComposite");
        assertEquals(MetaEcoreHelper.isRecord(javaClass), MetaEcoreHelper.isRecord(simonClass),
                () -> javaClass.getName() + " record");
        // Supertype names match
        var javaSuperNames = javaClass.getESuperTypes().stream().map(EClass::getName).sorted().toList();
        var simonSuperNames = simonClass.getESuperTypes().stream().map(EClass::getName).sorted().toList();
        assertEquals(javaSuperNames, simonSuperNames, () -> javaClass.getName() + " supertypes");
        // Features: same names and multiplicities (own only — supertypes contribute their own)
        for (EStructuralFeature javaFeature : javaClass.getEStructuralFeatures()) {
            EStructuralFeature simonFeature = simonClass.getEStructuralFeature(javaFeature.getName());
            assertNotNull(simonFeature,
                    () -> "Missing feature " + javaClass.getName() + "." + javaFeature.getName());
            assertEquals(javaFeature.getLowerBound(), simonFeature.getLowerBound(),
                    () -> javaClass.getName() + "." + javaFeature.getName() + " lowerBound");
            assertEquals(javaFeature.getUpperBound(), simonFeature.getUpperBound(),
                    () -> javaClass.getName() + "." + javaFeature.getName() + " upperBound");
            if (javaFeature instanceof EReference) {
                assertEquals(((EReference) javaFeature).isContainment(),
                        ((EReference) simonFeature).isContainment(),
                        () -> javaClass.getName() + "." + javaFeature.getName() + " containment");
                var javaTypeName = javaFeature.getEType() == null ? null : javaFeature.getEType().getName();
                var simonTypeName = simonFeature.getEType() == null ? null : simonFeature.getEType().getName();
                assertEquals(javaTypeName, simonTypeName,
                        () -> javaClass.getName() + "." + javaFeature.getName() + " type");
            }
        }
    }

    private static EClass eClassIn(EPackage pkg, String name) {
        EClassifier classifier = pkg.getEClassifier(name);
        assertNotNull(classifier, () -> "No classifier " + name + " in package " + pkg.getName());
        return (EClass) classifier;
    }

    private static EStructuralFeature findFeature(EClass owner, String name) {
        var direct = owner.getEStructuralFeature(name);
        return direct != null ? direct : owner.getEAllStructuralFeatures().stream()
                .filter(f -> name.equals(f.getName())).findFirst().orElse(null);
    }

    private static <E> E find(List<E> items, Predicate<E> p) {
        return items.stream().filter(p).findAny().orElse(null);
    }

    private static <E extends ENamedElement> E ensureValid(E element) {
        assertNotNull(element);
        var diag = new BasicDiagnostic();
        EcoreValidator.INSTANCE.validate(element, diag, Diagnostician.INSTANCE.createDefaultContext());
        assertTrue(diag.getSeverity() < Diagnostic.ERROR, diag::toString);
        return element;
    }

    @SuppressWarnings("unused")
    private static EObject ignore(EObject e) { return e; }
}
