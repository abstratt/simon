package com.abstratt.simon.tests;

import com.abstratt.simon.compiler.source.annotated.AnnotatedJava2EcoreMapper;
import com.abstratt.simon.examples.JavaExampleLanguages;
import com.abstratt.simon.tests.fixtures.TestHelper;
import org.eclipse.emf.ecore.EPackage;

import java.util.stream.Stream;

class JavaVsSimonMetamodelEquivalenceTest extends AbstractMetamodelEquivalenceTest {

    @Override
    protected Stream<String> packageNames() {
        return Stream.concat(super.packageNames(), Stream.of("Simon"));
    }

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
