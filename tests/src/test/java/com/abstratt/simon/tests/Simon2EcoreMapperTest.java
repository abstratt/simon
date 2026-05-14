package com.abstratt.simon.tests;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;

import com.abstratt.simon.compiler.source.simon.Simon2EcoreMapper;
import com.abstratt.simon.tests.fixtures.TestHelper;

/**
 * Tests {@link Simon2EcoreMapper} indirectly: drives the same mapper-shape
 * assertions as {@link AnnotatedJava2EcoreMapperTest}, but against the example metamodels
 * re-expressed in {@code @language Simon} and loaded once into
 * {@code TestHelper.SIMON_PACKAGES}.
 *
 * Resolves the EClassifier or EPackage corresponding to a Java
 * {@link ClassConstants} key by walking to the outermost enclosing class
 * (which names the Simon package, e.g. {@code UI}, {@code IM}, {@code UI2})
 * and looking the classifier up by simple name.
 */
public class Simon2EcoreMapperTest extends Source2EcoreMapperTest {

    @Override
    @SuppressWarnings("unchecked")
    protected <E extends ENamedElement> E build(Class<?> clazz) {
        Class<?> packageClass = clazz;
        while (packageClass.getEnclosingClass() != null) {
            packageClass = packageClass.getEnclosingClass();
        }
        EPackage simonPackage = TestHelper.SIMON_PACKAGES.get(packageClass.getSimpleName());
        if (simonPackage == null) {
            throw new IllegalStateException("No Simon package for " + packageClass.getSimpleName());
        }
        E result;
        if (packageClass == clazz) {
            result = (E) simonPackage;
        } else {
            EClassifier classifier = simonPackage.getEClassifier(clazz.getSimpleName());
            if (classifier == null) {
                throw new IllegalStateException(
                        "No classifier '" + clazz.getSimpleName() + "' in Simon package " + simonPackage.getName());
            }
            result = (E) classifier;
        }
        ensureValid(result);
        return result;
    }
}
