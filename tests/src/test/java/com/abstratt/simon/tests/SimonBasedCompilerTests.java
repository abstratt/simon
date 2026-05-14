package com.abstratt.simon.tests;

import org.eclipse.emf.ecore.EPackage;

import com.abstratt.simon.tests.fixtures.TestHelper;

/**
 * Runs {@link CompilerTests} against the example metamodels re-expressed in
 * {@code @language Simon} (loaded once into {@code TestHelper.SIMON_PACKAGES}).
 */
public class SimonBasedCompilerTests extends CompilerTests {

    @Override
    protected EPackage uiPackage() {
        return TestHelper.SIMON_PACKAGES.get("UI");
    }

    @Override
    protected EPackage ui2Package() {
        return TestHelper.SIMON_PACKAGES.get("UI2");
    }

    @Override
    protected EPackage imPackage() {
        return TestHelper.SIMON_PACKAGES.get("IM");
    }
}
