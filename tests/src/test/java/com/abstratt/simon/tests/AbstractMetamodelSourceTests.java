package com.abstratt.simon.tests;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;

import org.eclipse.emf.ecore.EPackage;
import org.junit.jupiter.api.Test;

import com.abstratt.simon.compiler.source.MetamodelSource;
import com.abstratt.simon.compiler.source.ecore.EPackageMetamodelSource;

/**
 * Shape tests for {@link MetamodelSource} implementations, parameterized by
 * the discovery mechanism that produces the underlying EPackages. Concrete
 * subclasses bind a pre-built UI package (for the
 * {@link EPackageMetamodelSource} wrapper test) and a discovery-driven
 * source factory (for the resolve / constrain tests).
 */
public abstract class AbstractMetamodelSourceTests {

    /** UI EPackage produced by the variant under test, used by the generic
     *  {@link EPackageMetamodelSource} wrapper test. */
    protected abstract EPackage uiPackage();

    /** Discovery-driven factory: classpath scan for the annotated-Java
     *  variant, {@code .simon}-file load for the Simon-source variant. */
    protected abstract MetamodelSource.Factory<?> dynamicSourceFactory();

    @Test
    void metamodelResolveType() {
        var metamodel = new EPackageMetamodelSource.Factory(uiPackage()).build();
        var resolved = metamodel.resolveType("Application", null);
        assertNotNull(resolved);
    }

    @Test
    void dynamicMetamodelSourceResolveType() {
        try (var typeSource = dynamicSourceFactory().build()) {
            var resolved = typeSource.resolveType("Application", null);
            assertNotNull(resolved);
        }
    }

    @Test
    void dynamicMetamodelResolveTypeConstrained() {
        try (var typeSource = dynamicSourceFactory().build()) {
            var resolved = typeSource.resolveType("Application", Collections.emptySet());
            assertNull(resolved);
        }
    }
}
