package com.abstratt.simon.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.abstratt.simon.compiler.Problem;
import com.abstratt.simon.compiler.source.URISourceProvider;

import com.abstratt.simon.compiler.source.ecore.EPackageMetamodelSource;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;

import com.abstratt.simon.compiler.Result;
import com.abstratt.simon.compiler.antlr.SimonCompilerAntlrImpl;
import com.abstratt.simon.compiler.source.ecore.EcoreDynamicMetamodelSource;
import com.abstratt.simon.compiler.target.ecore.EcoreModelBuilder;
import com.abstratt.simon.examples.kirra.Kirra;
import com.abstratt.simon.examples.ui.UI;
import com.abstratt.simon.metamodel.ecore.java2ecore.EcoreHelper;
import com.abstratt.simon.metamodel.ecore.java2ecore.Java2EcoreMapper;
import com.abstratt.simon.metamodel.ecore.java2ecore.MetaEcoreHelper;

public class TestHelper {

	public static final EPackage UI_PACKAGE = new Java2EcoreMapper().map(UI.class);
	public static final EPackage KIRRA_PACKAGE = new Java2EcoreMapper().map(Kirra.class);

	public static EClass uiClassFor(Class<?> clazz) {
		EPackage package_ = UI_PACKAGE;
		return eClassFor(clazz, package_);
	}

	public static EClass kirraClassFor(Class<?> clazz) {
		EPackage package_ = KIRRA_PACKAGE;
		return eClassFor(clazz, package_);
	}

	private static EClass eClassFor(Class<?> clazz, EPackage package_) {
		return (EClass) package_.getEClassifier(clazz.getSimpleName());
	}

	public static EObject compileUI(String toParse) {
		EPackageMetamodelSource.Factory typeSourceFactory = new EPackageMetamodelSource.Factory(UI_PACKAGE);
		return compile(typeSourceFactory, toParse);
	}

	private static EObject compile(EPackageMetamodelSource.Factory typeSourceFactory, String toParse) {
		var modelBuilder = new EcoreModelBuilder();
		var compiler = new SimonCompilerAntlrImpl<>(typeSourceFactory, modelBuilder);
		var result = compiler.compile(toParse);
		ensureSuccess(result);
		EObject rootObject = result.getRootObject();
		assertNotNull(rootObject);
		return rootObject;
	}

	public static EObject compileResourceToEObject(Class<?> packageClass, String path) throws Exception {
		List<Result<EObject>> results = compileResource(packageClass, path);
		ensureSuccess(results);
		EObject rootObject = results.get(0).getRootObject();
		assertNotNull(rootObject);
		return rootObject;
	}

	public static List<Result<EObject>> compileResource(Class<?> packageClass, String path) throws URISyntaxException {
		var resourceUrl = TestHelper.class.getResource(path);
		assertNotNull(resourceUrl, () -> "Resource not found: " + path);
		var resourceUri = resourceUrl.toURI();
		var baseURL = resourceUri.resolve(".");
		var sourceName = FilenameUtils.removeExtension(baseURL.relativize(resourceUri).getPath());
		var modelBuilder = new EcoreModelBuilder();
		var typeSource = new EcoreDynamicMetamodelSource.Factory(packageClass.getPackageName());
		var compiler = new SimonCompilerAntlrImpl<>(typeSource, modelBuilder);
		var results = compiler.compile(Arrays.asList(sourceName), new URISourceProvider(baseURL, "simon"));
		return results;
	}

	public static void ensureSuccess(Result<EObject> result) {
		assertEquals(0, result.getProblems().size(), result.getProblems()::toString);
	}
	public static void ensureSuccess(List<Result<EObject>> results) {
		var allProblems = new ArrayList<Problem>();
		results.stream().map(Result::getProblems).forEach(allProblems::addAll);
		assertEquals(0, allProblems.size(), allProblems::toString);
	}

	public static <P> P getPrimitiveValue(EObject element, String primitiveFeatureName) {
		Objects.requireNonNull(element);
		EObject value = EcoreHelper.getValue(element, primitiveFeatureName);
		if (value == null) {
			EStructuralFeature feature = EcoreHelper.findStructuralFeature(element, primitiveFeatureName);
			EStructuralFeature valueFeature = MetaEcoreHelper.getValueFeature((EClass) feature.getEType());
			Object defaultValue = valueFeature.getDefaultValue();
			return (P) defaultValue;
		}
		return EcoreHelper.unwrappedPrimitiveValue(value);
	}
}
