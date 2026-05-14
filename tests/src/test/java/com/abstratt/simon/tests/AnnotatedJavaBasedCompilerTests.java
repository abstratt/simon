package com.abstratt.simon.tests;

import org.eclipse.emf.ecore.EPackage;

import com.abstratt.simon.tests.fixtures.TestHelper;

/**
 * Runs {@link AbstractCompilerTests} against the Java-defined example metamodels
 * (UI, IM, UI2) built via {@code AnnotatedJava2EcoreMapper}.
 */
public class AnnotatedJavaBasedCompilerTests extends AbstractCompilerTests {

    @Override
    protected EPackage uiPackage() {
        return TestHelper.UI_PACKAGE;
    }

    @Override
    protected EPackage ui2Package() {
        return TestHelper.UI2_PACKAGE;
    }

    @Override
    protected EPackage imPackage() {
        return TestHelper.IM_PACKAGE;
    }
}
