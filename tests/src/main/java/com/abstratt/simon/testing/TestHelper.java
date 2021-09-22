package com.abstratt.simon.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;

import com.abstratt.simon.compiler.Problem;
import com.abstratt.simon.compiler.Result;
import com.abstratt.simon.compiler.antlr.SimonCompilerAntlrFactory;
import com.abstratt.simon.compiler.backend.ecore.EMFModelBackendFactory;
import com.abstratt.simon.compiler.source.MetamodelSource;
import com.abstratt.simon.compiler.source.MetamodelSource.Factory;
import com.abstratt.simon.compiler.source.MetamodelSourceChain;
import com.abstratt.simon.compiler.source.SimpleSourceProvider;
import com.abstratt.simon.compiler.source.SourceProvider;
import com.abstratt.simon.compiler.source.URISourceProvider;
import com.abstratt.simon.compiler.source.ecore.EPackageMetamodelSource;
import com.abstratt.simon.compiler.source.ecore.EcoreDynamicMetamodelSource;
import com.abstratt.simon.compiler.source.ecore.Java2EcoreMapper;
import com.abstratt.simon.compiler.source.ecore.ResourceMetamodelSource;
import com.abstratt.simon.examples.IM;
import com.abstratt.simon.examples.UI;
import com.abstratt.simon.examples.UI2;
import com.abstratt.simon.metamodel.ecore.EcoreMetamodel.EcoreType;
import com.abstratt.simon.metamodel.ecore.impl.EcoreHelper;
import com.abstratt.simon.metamodel.ecore.impl.MetaEcoreHelper;

public class TestHelper {

    public static final EPackage IM_PACKAGE = new Java2EcoreMapper().map(IM.class);
    public static final EPackage UI_PACKAGE = new Java2EcoreMapper().map(UI.class);
    public static final EPackage UI2_PACKAGE = new Java2EcoreMapper().map(UI2.class);
    private static SimonCompilerAntlrFactory compilerFactory = new SimonCompilerAntlrFactory();
    private static EMFModelBackendFactory backendFactory = new EMFModelBackendFactory();

    public static List<Result<EObject>> compileProject(EPackage package_, String toParse) {
        return compileProject(toParse, buildMetamodelSourceFactory(Arrays.asList(package_)));
    }

    public static List<Result<EObject>> compileProject(Resource resource, String source) {
        return compileProject(source, buildMetamodelSourceFactory(resource));
    }

    public static List<Result<EObject>> compileProject(String toParse,
            MetamodelSource.Factory<EcoreType<? extends EClassifier>> metamodelSourceFactory) {
        var toParse1 = Collections.singletonMap("source", toParse);
        return compileProject(new ArrayList<>(toParse1.keySet()), metamodelSourceFactory,
                buildSourceProvider(toParse1));
    }

    public static SimpleSourceProvider buildSourceProvider(Map<String, String> toParse) {
        return new SimpleSourceProvider(toParse);
    }

    public static MetamodelSourceChain.Factory<EcoreType<? extends EClassifier>> buildMetamodelSourceFactory(
            List<EPackage> packages) {
        List<Factory<EcoreType<? extends EClassifier>>> sourceFactories = packages.stream()
                .map(p -> new EPackageMetamodelSource.Factory(p)).collect(Collectors.toList());
        var typeSourceFactory = new MetamodelSourceChain.Factory<EcoreType<? extends EClassifier>>(sourceFactories);
        return typeSourceFactory;
    }

    public static List<Result<EObject>> compileResource(Class<?> packageClass, String path) throws URISyntaxException {
        var resourceUrl = TestHelper.class.getResource(path);
        assertNotNull(resourceUrl, () -> "Resource not found: " + path);
        var resourceUri = resourceUrl.toURI();
        var baseURL = resourceUri.resolve(".");
        var sourceName = FilenameUtils.removeExtension(baseURL.relativize(resourceUri).getPath());
        var entryPoints = Arrays.asList(sourceName);
        var sourceProvider = new URISourceProvider(baseURL, "simon");
        var typeSourceFactory = new EcoreDynamicMetamodelSource.Factory(packageClass.getPackageName());
        return compileProject(entryPoints, typeSourceFactory, sourceProvider);
    }

    public static List<Result<EObject>> compileProject(List<String> entryPoints,
            MetamodelSource.Factory<?> typeSourceFactory, SourceProvider sourceProvider) {
        var modelBuilder = backendFactory.create();
        var compiler = compilerFactory.create(typeSourceFactory, modelBuilder);
        return compiler.compile(entryPoints, sourceProvider);
    }

    public static EObject root(List<Result<EObject>> results) {
        return results.get(0).getRootObject();
    }

    public static EObject compileUsingIM(String toParse) {
        String[] toParse1 = { "@language IM " + toParse };
        return root(ensureSuccess(compileProject(Arrays.asList(IM_PACKAGE), toParse1)));
    }

    public static EObject compileUsingUI(String toParse) {
        String[] toParse1 = { "@language UI " + toParse };
        return root(ensureSuccess(compileProject(Arrays.asList(UI_PACKAGE), toParse1)));
    }

    public static List<Result<EObject>> compileProject(List<EPackage> packages, String... toParse) {
        int[] index = { 0 };
        var allSources = Arrays.stream(toParse).collect(Collectors.toMap(it -> "source" + index[0]++, it -> it));
        var compiled = compileProject(packages, allSources);
        return compiled;
    }

    public static List<Result<EObject>> compileProject(List<EPackage> packages, Map<String, String> allSources) {
        return compileProject(new ArrayList<>(allSources.keySet()), buildMetamodelSourceFactory(packages),
                buildSourceProvider(allSources));
    }

    public static List<Result<EObject>> compileProject(Resource resource, Map<String, String> allSources) {
        return compileProject(new ArrayList(allSources.keySet()), buildMetamodelSourceFactory(resource),
                new SimpleSourceProvider(allSources));
    }

    public static MetamodelSource.Factory buildMetamodelSourceFactory(Resource resource) {
        return new ResourceMetamodelSource.Factory(resource);
    }

    private static EClass eClassFor(Class<?> clazz, EPackage package_) {
        return (EClass) package_.getEClassifier(clazz.getSimpleName());
    }

    public static List<Result<EObject>> ensureSuccess(List<Result<EObject>> results) {
        var allProblems = new ArrayList<Problem>();
        results.stream().map(Result::getProblems).forEach(allProblems::addAll);
        assertEquals(0, allProblems.size(), allProblems::toString);
        return results;
    }

    public static <P> P getPrimitiveValue(EObject element, String primitiveFeatureName) {
        Objects.requireNonNull(element);
        EObject value = EcoreHelper.getValue(element, primitiveFeatureName);
        if (value == null) {
            var feature = EcoreHelper.findStructuralFeature(element, primitiveFeatureName);
            var valueFeature = MetaEcoreHelper.getValueFeature((EClass) feature.getEType());
            var defaultValue = valueFeature.getDefaultValue();
            return (P) defaultValue;
        }
        return EcoreHelper.unwrappedPrimitiveValue(value);
    }

    public static EClass imClassFor(Class<?> clazz) {
        EPackage package_ = IM_PACKAGE;
        return eClassFor(clazz, package_);
    }

    public static EClass uiClassFor(Class<?> clazz) {
        EPackage package_ = UI_PACKAGE;
        return eClassFor(clazz, package_);
    }
}
