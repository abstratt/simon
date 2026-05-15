package com.abstratt.simon.tests;

import com.abstratt.simon.compiler.source.annotated.AnnotatedJava2EcoreMapper;
import com.abstratt.simon.examples.JavaExampleLanguages;
import com.abstratt.simon.tests.fixtures.TestHelper;
import org.eclipse.emf.ecore.EPackage;

class JavaVsSimonMetamodelEquivalenceTest extends AbstractMetamodelEquivalenceTest {

    @Override
    protected EPackage expected(String packageName) {
        try {
            Class<?> clazz = Class.forName(JavaExampleLanguages.class.getPackageName() + "." + packageName);
            return new AnnotatedJava2EcoreMapper().map(clazz);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected EPackage actual(String packageName) {
        EPackage simonPackage = TestHelper.SIMON_PACKAGES.get(packageName);
        if (simonPackage == null) {
            throw new IllegalStateException("No Simon package for " + packageName);
        }
        return simonPackage;
    }
}
