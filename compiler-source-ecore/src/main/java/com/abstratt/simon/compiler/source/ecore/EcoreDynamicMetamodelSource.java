package com.abstratt.simon.compiler.source.ecore;

import com.abstratt.simon.compiler.source.MetamodelSource;
import com.abstratt.simon.compiler.source.SimpleSourceProvider;
import com.abstratt.simon.compiler.source.SourceProvider;
import com.abstratt.simon.metamodel.dsl.Meta;
import com.abstratt.simon.metamodel.ecore.EcoreMetamodel.EcoreType;
import com.abstratt.simon.metamodel.ecore.java2ecore.EcoreHelper;
import com.abstratt.simon.metamodel.ecore.java2ecore.Java2EcoreMapper;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;

public class EcoreDynamicMetamodelSource implements MetamodelSource<EcoreType<EClassifier>> {

	private final Map<String, EPackage> packages;
	private final ScanResult scanResult;

	private EcoreDynamicMetamodelSource(String javaPackage) {
		scanResult = new ClassGraph()
        .enableAnnotationInfo()
		.acceptPackages(javaPackage)
        .scan();
		//System.out.println("URL: " + scanResult.getClasspathURLs());
		var packageImplementations = scanResult.getClassesWithAnnotation(Meta.Package.class);
		var mapper = new Java2EcoreMapper();
		this.packages = packageImplementations.loadClasses().stream()
				.collect(Collectors.toMap(Class::getName, mapper::map));
		//System.out.println("*** Packages:\n" + packages);
	}

	@Override
	public SourceProvider builtInSources() {
		var builtIns = packages.values().stream().map(p -> p.getEAnnotation("simon/builtIns")).filter(Objects::nonNull).map(EAnnotation::getDetails).map(EMap::map).reduce(new LinkedHashMap<>(), (a, b) -> { a.putAll(b); return a;});
		return new SimpleSourceProvider(builtIns);
	}

	@Override
	public void close() {
		scanResult.close();
	}

	@Override
	public EcoreType<EClassifier> resolveType(String typeName) {
		var packages = this.packages.values();
		return packages //
				.stream() //
				.map(ePackage -> EcoreHelper.findClassifierByName(ePackage, typeName)) //
				.filter(Objects::nonNull) //
				.map(eClass -> (EcoreType<EClassifier>) EcoreType.fromClassifier(eClass)) //
				.findAny().orElse(null);
	}

	@Override
	public Stream<EcoreType<EClassifier>> enumerate() {
		var packages = this.packages.values();
		return packages //
				.stream() //
				.flatMap(EcoreHelper::findAllClassifiers) //
				.map(eClass -> (EcoreType<EClassifier>) EcoreType.fromClassifier(eClass));
	}

	public static class Factory implements MetamodelSource.Factory<EcoreType<EClassifier>> {
		private final String packageName;

		public Factory(String packageName) {
			this.packageName = packageName;
		}

		@Override
		public MetamodelSource<EcoreType<EClassifier>> build() {
			return new EcoreDynamicMetamodelSource(packageName);
		}
		
	}
}
