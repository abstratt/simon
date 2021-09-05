package com.abstratt.simon.compiler.source.ecore;

import com.abstratt.simon.compiler.source.SimpleSourceProvider;
import com.abstratt.simon.compiler.source.SourceProvider;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;

import com.abstratt.simon.compiler.source.MetamodelSource;
import com.abstratt.simon.metamodel.ecore.java2ecore.EcoreHelper;
import com.abstratt.simon.metamodel.ecore.EcoreMetamodel.EcoreType;

import java.util.stream.Stream;

public class EPackageMetamodelSource implements MetamodelSource<EcoreType<? extends EClassifier>> {
	private final EPackage ePackage;

	private EPackageMetamodelSource(EPackage ePackage) {
		this.ePackage = ePackage;
	}

	@Override
	public SourceProvider builtInSources() {
		var builtIns = ePackage.getEAnnotation("simon/builtIns");
		if (builtIns == null)
			return SourceProvider.NULL;
		return new SimpleSourceProvider(builtIns.getDetails().map());
	}

	@Override
	public EcoreType<? extends EClassifier> resolveType(String typeName) {
		var classifier = EcoreHelper.findClassifierByName(ePackage, typeName);
		return classifier == null ? null : EcoreType.fromClassifier(classifier);
	}

	@Override
	public Stream<EcoreType<? extends EClassifier>> enumerate() {
		return EcoreHelper.findAllClassifiers(ePackage).map(EcoreType::fromClassifier);
	}

	public EPackage getPackage() {
		return ePackage;
	}
	
	public static class Factory implements MetamodelSource.Factory<EcoreType<? extends EClassifier>> {
		private final EPackage ePackage;
		public Factory(EPackage ePackage) {
			this.ePackage = ePackage;
		}
		@Override
		public MetamodelSource<EcoreType<? extends EClassifier>> build() {
			return new EPackageMetamodelSource(ePackage);
		}
	}
}