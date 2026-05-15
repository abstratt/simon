package com.abstratt.simon.tests;

import com.abstratt.simon.compiler.source.annotated.AnnotatedJava2EcoreMapper;
import com.abstratt.simon.examples.JavaExampleLanguages;
import com.abstratt.simon.examples.kotlin.KotlinExampleLanguages;
import org.eclipse.emf.ecore.EPackage;

class JavaVsKotlinMetamodelEquivalenceTest extends AbstractMetamodelEquivalenceTest {

    @Override
    protected EPackage expected(String packageName) {
        return map(JavaExampleLanguages.class.getPackageName(), packageName);
    }

    @Override
    protected EPackage actual(String packageName) {
        return map(KotlinExampleLanguages.class.getPackageName(), packageName);
    }

    private static EPackage map(String packageNamespace, String name) {
        try {
            Class<?> clazz = Class.forName(packageNamespace + "." + name);
            return new AnnotatedJava2EcoreMapper().map(clazz);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}
