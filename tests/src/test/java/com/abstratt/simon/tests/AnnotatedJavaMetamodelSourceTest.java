package com.abstratt.simon.tests;

import org.eclipse.emf.ecore.EPackage;

import com.abstratt.simon.compiler.source.MetamodelSource;
import com.abstratt.simon.compiler.source.annotated.AnnotatedJava2EcoreMapper;
import com.abstratt.simon.compiler.source.annotated.AnnotatedJavaMetamodelSource;
import com.abstratt.simon.examples.UI;
import com.abstratt.simon.tests.fixtures.TestHelper;

/**
 * Runs {@link AbstractMetamodelSourceTests} against
 * {@link AnnotatedJavaMetamodelSource}, which internally uses
 * {@link AnnotatedJava2EcoreMapper} on classpath-discovered
 * {@code @Meta.Package}-annotated Java types.
 */
public class AnnotatedJavaMetamodelSourceTest extends AbstractMetamodelSourceTests {

    @Override
    protected EPackage uiPackage() {
        return TestHelper.UI_PACKAGE;
    }

    @Override
    protected MetamodelSource.Factory<?> dynamicSourceFactory() {
        return new AnnotatedJavaMetamodelSource.Factory(UI.class.getPackageName());
    }
}
