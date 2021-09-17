package com.abstratt.simon.compiler.source.ecore;

import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;

import com.abstratt.simon.compiler.source.MetamodelSource;
import com.abstratt.simon.compiler.source.MetamodelSourceChain;
import com.abstratt.simon.metamodel.ecore.EcoreMetamodel.EcoreType;

public class ResourceMetamodelSource extends EPackageMetamodelSource {
	protected ResourceMetamodelSource(EPackage ePackage) {
		super(ePackage);
	}

	public static class Factory implements MetamodelSource.Factory<EcoreType<? extends EClassifier>> {
		private final Resource resource;
		public Factory(Resource resource) {
			this.resource = resource;
		}
		@Override
		public MetamodelSource<EcoreType<? extends EClassifier>> build() {
			var packages = resource.getContents().stream().map(e -> (EPackage) e);
			var childSources = packages.map(ResourceMetamodelSource::new).collect(Collectors.toList());
			return new MetamodelSourceChain(childSources);
		}
	}
}
