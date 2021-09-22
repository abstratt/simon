package com.abstratt.simon.compiler.source.ecore;

import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;

import com.abstratt.simon.compiler.source.MetamodelSource;
import com.abstratt.simon.compiler.source.SimpleSourceProvider;
import com.abstratt.simon.compiler.source.SourceProvider;
import com.abstratt.simon.metamodel.ecore.EcoreMetamodel.EcoreType;
import com.abstratt.simon.metamodel.ecore.impl.EcoreHelper;

public class EPackageMetamodelSource implements MetamodelSource<EcoreType<? extends EClassifier>> {
    private final EPackage ePackage;

    protected EPackageMetamodelSource(EPackage ePackage) {
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
    public EcoreType<? extends EClassifier> resolveType(String typeName, Set<String> languages) {
        if (languages != null && !languages.contains(ePackage.getName()))
            return null;
        var classifier = EcoreHelper.findClassifierByName(ePackage, typeName);
        return classifier == null ? null : EcoreType.fromClassifier(classifier);
    }

    @Override
    public Stream<EcoreType<? extends EClassifier>> enumerate(Set<String> languages) {
        if (languages != null && !languages.contains(ePackage.getName()))
            return Stream.empty();
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
