package com.abstratt.simon.compiler.source.simon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

import com.abstratt.simon.compiler.Problem;
import com.abstratt.simon.compiler.Result;
import com.abstratt.simon.compiler.antlr.SimonCompilerAntlrFactory;
import com.abstratt.simon.compiler.backend.ecore.EMFModelBackendFactory;
import com.abstratt.simon.compiler.source.MetamodelSource;
import com.abstratt.simon.compiler.source.SimpleSourceProvider;
import com.abstratt.simon.compiler.source.SourceProvider;
import com.abstratt.simon.compiler.source.java.AnnotatedJavaMetamodelSource;
import com.abstratt.simon.metamodel.ecore.EcoreMetamodel.EcoreType;
import com.abstratt.simon.metamodel.ecore.impl.EcoreHelper;

/**
 * A {@link MetamodelSource} that loads metamodels written in Simon source
 * ({@code @language Simon}).
 *
 * Bootstraps a sub-compiler against a caller-supplied Java-defined metamodel
 * package (typically {@code com.abstratt.simon.examples.Simon}), compiles
 * the given metamodel entry points, then maps the resulting
 * {@code Simon.Package} EObjects to Ecore EPackages via
 * {@link Simon2EcoreMapper}.
 */
public class SimonFileMetamodelSource implements MetamodelSource<EcoreType<EClassifier>> {

    private final Map<String, EPackage> packages;

    private SimonFileMetamodelSource(MetamodelSource.Factory<?> bootstrapSourceFactory,
            List<String> entryPoints, SourceProvider sources) {
        this.packages = buildPackages(bootstrapSourceFactory, entryPoints, sources);
    }

    private static Map<String, EPackage> buildPackages(MetamodelSource.Factory<?> bootstrapSourceFactory,
            List<String> entryPoints, SourceProvider sources) {
        var backendFactory = new EMFModelBackendFactory();
        var compilerFactory = new SimonCompilerAntlrFactory();
        var backend = backendFactory.create();
        var compiler = compilerFactory.<EObject>create(bootstrapSourceFactory, backend);
        List<Result<EObject>> results = compiler.compile(entryPoints, sources);
        List<Problem> problems = results.stream()
                .flatMap(r -> r.getProblems().stream())
                .collect(Collectors.toList());
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Errors compiling Simon metamodel sources: " + problems);
        }
        // Each entry point yields its own root Simon.Package. Imports pull
        // additional packages into the same Resource — we want all of them.
        Collection<EObject> packageInstances = collectAllPackages(results);
        return new Simon2EcoreMapper().map(packageInstances);
    }

    private static Collection<EObject> collectAllPackages(List<Result<EObject>> results) {
        var seen = new LinkedHashMap<EObject, Boolean>();
        for (Result<EObject> result : results) {
            EObject root = result.getRootObject();
            if (root == null) {
                continue;
            }
            seen.put(root, Boolean.TRUE);
            if (root.eResource() != null) {
                for (EObject content : root.eResource().getContents()) {
                    seen.put(content, Boolean.TRUE);
                }
            }
        }
        return new ArrayList<>(seen.keySet());
    }

    @Override
    public EcoreType<EClassifier> resolveType(String typeName, Set<String> languages) {
        return enabledPackages(languages)
                .map(p -> EcoreHelper.findClassifierByName(p, typeName))
                .filter(Objects::nonNull)
                .map(c -> (EcoreType<EClassifier>) EcoreType.fromClassifier(c))
                .findAny().orElse(null);
    }

    @Override
    public Stream<EcoreType<EClassifier>> enumerate(Set<String> languages) {
        return enabledPackages(languages)
                .flatMap(EcoreHelper::findAllClassifiers)
                .map(c -> (EcoreType<EClassifier>) EcoreType.fromClassifier(c));
    }

    @Override
    public SourceProvider builtInSources() {
        return new SimpleSourceProvider(Map.of());
    }

    private Stream<EPackage> enabledPackages(Set<String> languages) {
        return packages.values().stream()
                .filter(p -> languages == null || languages.contains(p.getName()));
    }

    public Map<String, EPackage> getPackages() {
        return packages;
    }

    public static class Factory implements MetamodelSource.Factory<EcoreType<EClassifier>> {
        private final MetamodelSource.Factory<?> bootstrapSourceFactory;
        private final List<String> entryPoints;
        private final SourceProvider sources;

        public Factory(MetamodelSource.Factory<?> bootstrapSourceFactory,
                List<String> entryPoints, SourceProvider sources) {
            this.bootstrapSourceFactory = bootstrapSourceFactory;
            this.entryPoints = entryPoints;
            this.sources = sources;
        }

        /**
         * Convenience constructor: derives the bootstrap source factory from
         * any Java class whose enclosing package contains a
         * {@code @Meta.Package}-annotated type (typically the Simon bootstrap).
         */
        public static Factory withBootstrapClass(Class<?> bootstrapClass,
                List<String> entryPoints, SourceProvider sources) {
            return new Factory(
                    new AnnotatedJavaMetamodelSource.Factory(bootstrapClass.getPackageName()),
                    entryPoints, sources);
        }

        @Override
        public MetamodelSource<EcoreType<EClassifier>> build() {
            return new SimonFileMetamodelSource(bootstrapSourceFactory, entryPoints, sources);
        }
    }
}
