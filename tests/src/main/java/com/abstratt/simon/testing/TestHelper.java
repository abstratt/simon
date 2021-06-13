package com.abstratt.simon.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URL;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;

import com.abstratt.simon.compiler.SimonCompiler;
import com.abstratt.simon.compiler.TypeSource;
import com.abstratt.simon.compiler.Configuration.Provider;
import com.abstratt.simon.compiler.SimonCompiler.Result;
import com.abstratt.simon.compiler.ecore.EPackageTypeSource;
import com.abstratt.simon.compiler.ecore.EcoreDynamicTypeSource;
import com.abstratt.simon.compiler.ecore.EcoreHelper;
import com.abstratt.simon.compiler.ecore.EcoreMetamodel.EcoreObjectType;
import com.abstratt.simon.compiler.ecore.EcoreMetamodel.EcoreSlotted;
import com.abstratt.simon.compiler.ecore.EcoreModelBuilder;
import com.abstratt.simon.examples.Kirra;
import com.abstratt.simon.examples.UI;
import com.abstratt.simon.metamodel.dsl.java2ecore.Java2EcoreMapper;
import com.abstratt.simon.metamodel.dsl.java2ecore.MetaEcoreHelper;

public class TestHelper {

	public static EPackage UI_PACKAGE = new Java2EcoreMapper().map(UI.class);
	public static EPackage KIRRA_PACKAGE = new Java2EcoreMapper().map(Kirra.class);

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
		EPackageTypeSource typeSource = new EPackageTypeSource(UI_PACKAGE);
		return compile(typeSource, toParse);
	}

	private static EObject compile(EPackageTypeSource typeSource, String toParse) {
		Provider<EcoreObjectType, EcoreSlotted<?>, EObject> modelBuilder = new EcoreModelBuilder();
		SimonCompiler<EObject> compiler = new SimonCompiler<EObject>(typeSource, modelBuilder);
		Result<EObject> result = compiler.compile(toParse);
		ensureSuccess(result);
		EObject rootObject = result.getRootObject();
		assertNotNull(rootObject);
		return rootObject;
	}

	public static EObject compileResourceToEObject(String path) {
		Result<EObject> result = compileResource(path);
		ensureSuccess(result);
		EObject rootObject = result.getRootObject();
		assertNotNull(rootObject);
		return rootObject;
	}

	public static Result<EObject> compileResource(String path) {
		URL resourceUrl = TestHelper.class.getResource(path);
		assertNotNull(resourceUrl, () -> "Resource not found: " + path);
		Provider<EcoreObjectType, EcoreSlotted<?>, EObject> modelBuilder = new EcoreModelBuilder();
		TypeSource<?> typeSource = new EcoreDynamicTypeSource();
		SimonCompiler<EObject> compiler = new SimonCompiler<EObject>(typeSource, modelBuilder);
		Result<EObject> result = compiler.compile(resourceUrl);
		return result;
	}

	private static void ensureSuccess(Result<EObject> result) {
		assertEquals(0, result.getProblems().size(), result.getProblems()::toString);
	}
	
	public static <P> P getPrimitiveValue(EObject element, String primitiveFeatureName) {
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
