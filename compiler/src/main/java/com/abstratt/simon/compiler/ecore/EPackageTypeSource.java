package com.abstratt.simon.compiler.ecore;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;

import com.abstratt.simon.compiler.TypeSource;
import com.abstratt.simon.compiler.ecore.EcoreMetamodel.EcoreType;

public class EPackageTypeSource implements TypeSource<EcoreType<? extends EClassifier>> {
	private final EPackage ePackage;

	public EPackageTypeSource(EPackage ePackage) {
		this.ePackage = ePackage;
	}

	@Override
	public EcoreType<? extends EClassifier> resolveType(String typeName) {
		EClassifier classifier = ePackage.getEClassifier(typeName);
		return classifier == null ? null : EcoreType.fromClassifier(classifier);
	}
	
	public EPackage getPackage() {
		return ePackage;
	}
}
