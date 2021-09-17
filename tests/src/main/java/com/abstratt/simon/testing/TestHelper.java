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

import com.abstratt.simon.compiler.Problem;
import com.abstratt.simon.compiler.Result;
import com.abstratt.simon.compiler.antlr.SimonCompilerAntlrFactory;
import com.abstratt.simon.compiler.backend.ecore.EMFModelBackendFactory;
import com.abstratt.simon.compiler.source.MetamodelSource.Factory;
import com.abstratt.simon.compiler.source.MetamodelSourceChain;
import com.abstratt.simon.compiler.source.SimpleSourceProvider;
import com.abstratt.simon.compiler.source.URISourceProvider;
import com.abstratt.simon.compiler.source.ecore.EPackageMetamodelSource;
import com.abstratt.simon.compiler.source.ecore.EcoreDynamicMetamodelSource;
import com.abstratt.simon.compiler.source.ecore.Java2EcoreMapper;
import com.abstratt.simon.examples.kirra.Kirra;
import com.abstratt.simon.examples.ui.UI;
import com.abstratt.simon.metamodel.ecore.EcoreMetamodel.EcoreType;
import com.abstratt.simon.metamodel.ecore.impl.EcoreHelper;
import com.abstratt.simon.metamodel.ecore.impl.MetaEcoreHelper;

public class TestHelper {

	public static final EPackage KIRRA_PACKAGE = new Java2EcoreMapper().map(Kirra.class);
	public static final EPackage UI_PACKAGE = new Java2EcoreMapper().map(UI.class);
	private static SimonCompilerAntlrFactory compilerFactory = new SimonCompilerAntlrFactory();
	private static EMFModelBackendFactory backendFactory = new EMFModelBackendFactory();

	public static EObject compile(EPackage package_, String toParse) {
        return compile(Arrays.asList(package_), toParse);
    }

	public static EObject compile_(EPackageMetamodelSource.Factory typeSourceFactory, String toParse) {
		var modelBuilder = backendFactory.create();
		var compiler = compilerFactory.create(typeSourceFactory, modelBuilder);
		var result = compiler.compile(toParse);
		ensureSuccess(result);
		EObject rootObject = result.getRootObject();
		assertNotNull(rootObject);
		return rootObject;
	}

	public static EObject compile(List<EPackage> asList, String toParse) {
        var models = compileValidProject(asList, toParse);
        assertNotNull(models.get(0));
        return models.get(0).getRootObject();
    }

	public static List<Result<EObject>> compileProject(EPackage package_, String toParse) {
		return compileProject(package_, Collections.singletonMap("source", toParse));
	}

	public static List<Result<EObject>> compileProject(EPackage package_, Map<String, String> toParse) {
		return compileProject(Arrays.asList(package_), new ArrayList<>(toParse.keySet()), toParse);
	}

	public static List<Result<EObject>> compileProject(EPackage package_, List<String> entryPoints, Map<String, String> toParse) {
        return compileProject(Arrays.asList(package_), entryPoints, toParse);
    }

	public static List<Result<EObject>> compileProject(List<EPackage> packages, List<String> entryPoints,
            Map<String, String> toParse)
    {
        List<Factory<EcoreType<? extends EClassifier>>> sourceFactories = packages.stream().map(p -> new EPackageMetamodelSource.Factory(p)).collect(Collectors.toList());
        // TODO-RC this should be supported in the compiler itself, which should take
        // multiple type sources (one per language) and then decide which ones to enable
        // based on the languages declared in the file using the @language processing instruction
        var typeSourceFactory = new MetamodelSourceChain.Factory<EcoreType<? extends EClassifier>>(sourceFactories);
        var modelBuilder = backendFactory.create();
        var compiler = compilerFactory.create(typeSourceFactory, modelBuilder);
        return compiler.compile(entryPoints, new SimpleSourceProvider(toParse));
    }

	public static List<Result<EObject>> compileResource(Class<?> packageClass, String path) throws URISyntaxException {
		var resourceUrl = TestHelper.class.getResource(path);
		assertNotNull(resourceUrl, () -> "Resource not found: " + path);
		var resourceUri = resourceUrl.toURI();
		var baseURL = resourceUri.resolve(".");
		var sourceName = FilenameUtils.removeExtension(baseURL.relativize(resourceUri).getPath());
		var modelBuilder = backendFactory.create();
		var typeSource = new EcoreDynamicMetamodelSource.Factory(packageClass.getPackageName());
		var compiler = compilerFactory.create(typeSource, modelBuilder);
		var results = compiler.compile(Arrays.asList(sourceName), new URISourceProvider(baseURL, "simon"));
		return results;
	}

	public static EObject compileResourceToEObject(Class<?> packageClass, String path) throws Exception {
		List<Result<EObject>> results = compileResource(packageClass, path);
		ensureSuccess(results);
		EObject rootObject = results.get(0).getRootObject();
		assertNotNull(rootObject);
		return rootObject;
	}
	
    public static EObject compileUsingKirra(String toParse) {
        return compile(KIRRA_PACKAGE, "@language Kirra " + toParse);
    }	
    
    public static EObject compileUsingUI(String toParse) {
        return compile(UI_PACKAGE, "@language UI " + toParse);
    }

    public static List<Result<EObject>> compileValidProject(EPackage package_, List<String> entryPoints, Map<String, String> toParse) {
        return compileValidProject(Arrays.asList(package_), entryPoints, toParse);
    }

    public static List<Result<EObject>> compileValidProject(EPackage package_, String... toParse) {
        return compileValidProject(Arrays.asList(package_), toParse);
    }

    public static List<Result<EObject>> compileValidProject(List<EPackage> packages, List<String> entryPoints,
            Map<String, String> toParse)
    {
        var results = compileProject(packages, entryPoints, toParse);
        results.forEach(TestHelper::ensureSuccess);
        return results;
    }

    public static List<Result<EObject>> compileValidProject(List<EPackage> packages, String... toParse) {
        int[] index = {0};
        var allSources = Arrays.stream(toParse).collect(Collectors.toMap(it -> "source" + index[0]++, it -> it));
        return compileValidProject(packages, new ArrayList<>(allSources.keySet()), allSources);
    }

    private static EClass eClassFor(Class<?> clazz, EPackage package_) {
		return (EClass) package_.getEClassifier(clazz.getSimpleName());
	}

    public static void ensureSuccess(List<Result<EObject>> results) {
		var allProblems = new ArrayList<Problem>();
		results.stream().map(Result::getProblems).forEach(allProblems::addAll);
		assertEquals(0, allProblems.size(), allProblems::toString);
	}

    public static void ensureSuccess(Result<?> result) {
		assertEquals(0, result.getProblems().size(), result.getProblems()::toString);
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
	public static EClass kirraClassFor(Class<?> clazz) {
		EPackage package_ = KIRRA_PACKAGE;
		return eClassFor(clazz, package_);
	}

	public static EClass uiClassFor(Class<?> clazz) {
		EPackage package_ = UI_PACKAGE;
		return eClassFor(clazz, package_);
	}
}
