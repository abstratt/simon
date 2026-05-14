package com.abstratt.simon.tests;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;

import com.abstratt.simon.compiler.source.ClasspathSourceProvider;
import com.abstratt.simon.compiler.source.simon.SimonFileMetamodelSource;
import com.abstratt.simon.examples.Simon;
import com.abstratt.simon.examples.UI;
import com.abstratt.simon.tests.fixtures.TestHelper;

/**
 * Runs {@link CompilerTests} against the same example metamodels expressed
 * in {@code @language Simon} and loaded at runtime via
 * {@link SimonFileMetamodelSource}.
 *
 * The Simon-defined IM package inherits the {@code simon/builtIns}
 * annotation from its Java counterpart so the {@code @import 'im'}
 * directive in the test sources resolves to the same {@code im.simon}
 * prelude content. Without that copy, IM tests using primitive types
 * (e.g. {@code im.StringValue}) would fail to resolve.
 */
public class SimonCompilerTests extends CompilerTests {

    private static final Map<String, EPackage> PACKAGES = loadPackages();

    private static Map<String, EPackage> loadPackages() {
        var sources = new ClasspathSourceProvider(UI.class.getClassLoader(),
                "com/abstratt/simon/examples", "simon");
        var factory = SimonFileMetamodelSource.Factory.withBootstrapClass(
                Simon.class,
                Arrays.asList("ui", "ui2", "ui3", "im-metamodel", "daui"),
                sources);
        try (var source = factory.build()) {
            var packages = new LinkedHashMap<>(((SimonFileMetamodelSource) source).getPackages());
            copyBuiltInsAnnotation(TestHelper.IM_PACKAGE, packages.get("IM"));
            return packages;
        }
    }

    /**
     * Copies the {@code simon/builtIns} annotation (if any) from the source
     * EPackage onto the target. This lets a Simon-defined package satisfy
     * {@code @import 'im'}-style directives by reusing the same prelude
     * content as its Java-defined twin.
     */
    private static void copyBuiltInsAnnotation(EPackage from, EPackage to) {
        if (from == null || to == null) return;
        EAnnotation original = from.getEAnnotation("simon/builtIns");
        if (original == null) return;
        EAnnotation copy = EcoreFactory.eINSTANCE.createEAnnotation();
        copy.setSource(original.getSource());
        copy.getDetails().putAll(original.getDetails().map());
        to.getEAnnotations().add(copy);
    }

    @Override
    protected EPackage uiPackage() {
        return PACKAGES.get("UI");
    }

    @Override
    protected EPackage ui2Package() {
        return PACKAGES.get("UI2");
    }

    @Override
    protected EPackage imPackage() {
        return PACKAGES.get("IM");
    }
}
