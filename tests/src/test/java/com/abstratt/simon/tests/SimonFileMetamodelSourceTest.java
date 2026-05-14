package com.abstratt.simon.tests;

import java.util.Arrays;

import org.eclipse.emf.ecore.EPackage;

import com.abstratt.simon.compiler.source.ClasspathSourceProvider;
import com.abstratt.simon.compiler.source.MetamodelSource;
import com.abstratt.simon.compiler.source.simon.Simon2EcoreMapper;
import com.abstratt.simon.compiler.source.simon.SimonFileMetamodelSource;
import com.abstratt.simon.examples.Simon;
import com.abstratt.simon.examples.UI;
import com.abstratt.simon.tests.fixtures.TestHelper;

/**
 * Runs {@link AbstractMetamodelSourceTests} against
 * {@link SimonFileMetamodelSource}, which internally uses
 * {@link Simon2EcoreMapper} to load the {@code .simon}-defined
 * example metamodels from the classpath.
 */
public class SimonFileMetamodelSourceTest extends AbstractMetamodelSourceTests {

    @Override
    protected EPackage uiPackage() {
        return TestHelper.SIMON_PACKAGES.get("UI");
    }

    @Override
    protected MetamodelSource.Factory<?> dynamicSourceFactory() {
        var sources = new ClasspathSourceProvider(UI.class.getClassLoader(),
                "com/abstratt/simon/examples", "simon");
        return SimonFileMetamodelSource.Factory.withBootstrapClass(
                Simon.class,
                Arrays.asList("ui", "ui2", "ui3", "im-metamodel", "daui"),
                sources);
    }
}
