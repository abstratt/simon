package com.abstratt.simon.tests;

import static com.abstratt.simon.tests.TestHelper.UI_PACKAGE;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.abstratt.simon.compiler.source.ecore.EPackageMetamodelSource;
import com.abstratt.simon.compiler.source.ecore.EcoreDynamicMetamodelSource;
import com.abstratt.simon.examples.UI;

public class MetamodelSourceTests {

    @Test
    void metamodelResolveType() {
        var metamodel = new EPackageMetamodelSource.Factory(UI_PACKAGE).build();
        var resolved = metamodel.resolveType("Application", null);
        assertNotNull(resolved);
    }

    @Test
    void dynamicMetamodelSourceResolveType() {
        var typeSourceFactory = new EcoreDynamicMetamodelSource.Factory(UI.class.getPackageName());
        try (var typeSource = typeSourceFactory.build()) {
            var resolved = typeSource.resolveType("Application", null);
            assertNotNull(resolved);
        }
    }

    @Test
    void dynamicMetamodelResolveTypeConstrained() {
        var typeSourceFactory = new EcoreDynamicMetamodelSource.Factory(UI.class.getPackageName());
        try (var typeSource = typeSourceFactory.build()) {
            var resolved = typeSource.resolveType("Application", Collections.emptySet());
            assertNull(resolved);
        }
    }
}
