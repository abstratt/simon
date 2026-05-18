package com.abstratt.simon.tests;

import org.eclipse.emf.ecore.*;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Asserts that two metamodel-source pipelines produce equivalent EPackages
 * for the five example languages. Concrete subclasses pick which pair to
 * compare by overriding {@link #expected(String)} and {@link #actual(String)}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractMetamodelEquivalenceTest {

    protected abstract EPackage expected(String packageName);
    protected abstract EPackage actual(String packageName);

    protected Stream<String> packageNames() {
        return Stream.of("UI", "UI2", "UI3", "IM", "DAUI");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("packageNames")
    void equivalent(String packageName) {
        MetamodelEquivalence.assertEquivalent(expected(packageName), actual(packageName));
    }

    /**
     * Asserts structural equivalence between two EPackages produced by different
     * source mappers. Compares by name, recursively; ignores nsURI/nsPrefix and
     * declaration order.
     */
    private static final class MetamodelEquivalence {

        private MetamodelEquivalence() {}

        static void assertEquivalent(EPackage expected, EPackage actual) {
            String at = "EPackage[" + (expected == null ? "?" : expected.getName()) + "]";
            assertNotNull(expected, at + ": expected is null");
            assertNotNull(actual, at + ": actual is null");
            assertEquals(expected.getName(), actual.getName(), at + ": name");

            Map<String, EClassifier> expectedByName = byName(expected.getEClassifiers());
            Map<String, EClassifier> actualByName = byName(actual.getEClassifiers());
            assertEquals(expectedByName.keySet(), actualByName.keySet(),
                    at + ": classifier names");

            for (String name : expectedByName.keySet()) {
                assertClassifierEquivalent(at, expectedByName.get(name), actualByName.get(name));
            }

            assertBuiltInsEquivalent(at, expected, actual);
        }

        private static void assertClassifierEquivalent(String at, EClassifier expected, EClassifier actual) {
            String here = at + " :: " + expected.getName();
            assertEquals(metaKind(expected), metaKind(actual), here + ": metaclass");
            if (expected instanceof EClass) {
                assertEClassEquivalent(here, (EClass) expected, (EClass) actual);
            } else if (expected instanceof EEnum) {
                assertEEnumEquivalent(here, (EEnum) expected, (EEnum) actual);
            } else {
                fail(here + ": unsupported classifier kind " + expected.getClass().getName());
            }
        }

        private static void assertEClassEquivalent(String at, EClass expected, EClass actual) {
            assertEquals(expected.isAbstract(), actual.isAbstract(), at + ": abstract");
            assertEquals(expected.isInterface(), actual.isInterface(), at + ": interface");

            Set<String> expectedSupers = expected.getESuperTypes().stream()
                    .map(EClass::getName).collect(Collectors.toCollection(TreeSet::new));
            Set<String> actualSupers = actual.getESuperTypes().stream()
                    .map(EClass::getName).collect(Collectors.toCollection(TreeSet::new));
            assertEquals(expectedSupers, actualSupers, at + ": supertypes");

            Map<String, EStructuralFeature> expectedFeatures = byName(expected.getEStructuralFeatures());
            Map<String, EStructuralFeature> actualFeatures = byName(actual.getEStructuralFeatures());
            assertEquals(expectedFeatures.keySet(), actualFeatures.keySet(),
                    at + ": declared feature names");

            for (String name : expectedFeatures.keySet()) {
                assertFeatureEquivalent(at, expectedFeatures.get(name), actualFeatures.get(name));
            }
        }

        private static void assertFeatureEquivalent(String at, EStructuralFeature expected, EStructuralFeature actual) {
            String here = at + "." + expected.getName();
            assertEquals(metaKind(expected), metaKind(actual), here + ": metaclass");
            assertEquals(typeName(expected), typeName(actual), here + ": type");
            assertEquals(expected.getLowerBound(), actual.getLowerBound(), here + ": lowerBound");
            assertEquals(expected.getUpperBound(), actual.getUpperBound(), here + ": upperBound");
            if (expected instanceof EReference) {
                EReference er = (EReference) expected;
                EReference ar = (EReference) actual;
                assertEquals(er.isContainment(), ar.isContainment(), here + ": containment");
                assertEquals(er.isContainer(), ar.isContainer(), here + ": container");
                assertEquals(oppositeName(er), oppositeName(ar), here + ": eOpposite");
            }
        }

        private static void assertEEnumEquivalent(String at, EEnum expected, EEnum actual) {
            Set<String> expectedLiterals = expected.getELiterals().stream()
                    .map(EEnumLiteral::getName).collect(Collectors.toCollection(TreeSet::new));
            Set<String> actualLiterals = actual.getELiterals().stream()
                    .map(EEnumLiteral::getName).collect(Collectors.toCollection(TreeSet::new));
            assertEquals(expectedLiterals, actualLiterals, at + ": enum literals");
        }

        private static void assertBuiltInsEquivalent(String at, EPackage expected, EPackage actual) {
            EAnnotation e = expected.getEAnnotation("simon/builtIns");
            EAnnotation a = actual.getEAnnotation("simon/builtIns");
            if (e == null && a == null) return;
            assertNotNull(e, at + ": expected has no simon/builtIns but actual does");
            assertNotNull(a, at + ": actual has no simon/builtIns but expected does");
            Map<String, String> eDetails = new TreeMap<>(e.getDetails().map());
            Map<String, String> aDetails = new TreeMap<>(a.getDetails().map());
            assertEquals(eDetails.keySet(), aDetails.keySet(), at + ": simon/builtIns keys");
            for (String key : eDetails.keySet()) {
                assertEquals(eDetails.get(key).trim(), aDetails.get(key).trim(),
                        at + ": simon/builtIns[" + key + "]");
            }
        }

        private static String metaKind(EClassifier c) {
            if (c instanceof EClass) return "EClass";
            if (c instanceof EEnum) return "EEnum";
            return c.eClass().getName();
        }

        private static String metaKind(EStructuralFeature f) {
            if (f instanceof EReference) return "EReference";
            return "EAttribute";
        }

        private static String typeName(EStructuralFeature f) {
            EClassifier type = f.getEType();
            return type == null ? null : type.getName();
        }

        private static String oppositeName(EReference ref) {
            EReference opp = ref.getEOpposite();
            return opp == null ? null : opp.getName();
        }

        private static <E extends ENamedElement> Map<String, E> byName(List<? extends E> elements) {
            Map<String, E> map = new TreeMap<>();
            for (E e : elements) {
                map.put(e.getName(), e);
            }
            return map;
        }
    }
}
