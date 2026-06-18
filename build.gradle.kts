// Root Gradle build for Simon — a side-by-side alternative to the Maven build.
//
// Common per-module configuration lives in the buildSrc convention plugins
// (simon.java-conventions / simon.kotlin-conventions), which each module applies directly. This
// keeps the build compatible with Isolated Projects (no cross-project configuration here).
//
// The root build only owns the cross-module aggregation: the aggregated Javadoc task. It feeds on
// resolvable configurations that pull artifacts the modules publish, rather than reaching into the
// modules' models.

import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment

repositories {
    mavenCentral()
}

// All modules (same set as settings.gradle.kts). Declared statically so the root never iterates or
// configures subprojects at execution time.
val allModules = listOf(
    ":gen-utils",
    ":metamodel",
    ":annotation-dsl",
    ":annotation-processor",
    ":compiler-source",
    ":compiler-backend",
    ":compiler",
    ":antlr-parser",
    ":antlr-compiler",
    ":metamodel-ecore",
    ":compiler-source-ecore",
    ":compiler-source-annotated-java",
    ":compiler-backend-ecore",
    ":compiler-source-simon",
    ":example-languages",
    ":example-languages-kotlin",
    ":test-fixtures",
    ":tests",
)

// Runtime classpath for the aggregated Javadoc: every module's runtime dependencies. Standard JVM
// runtime attributes so variant-aware dependencies (e.g. guava jre vs android) resolve correctly.
val aggregateJavadocClasspath = configurations.create("aggregateJavadocClasspath") {
    isCanBeResolved = true
    isCanBeConsumed = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        attribute(
            TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
            objects.named(TargetJvmEnvironment.STANDARD_JVM),
        )
    }
}

// The hand-written main Java source directories of every module, shared as artifacts by the
// convention plugin's `javadocSourceDirs` consumable configuration.
val aggregateJavadocSourceDirs = configurations.create("aggregateJavadocSourceDirs") {
    isCanBeResolved = true
    isCanBeConsumed = false
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
        attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named("javadoc-source-dirs"))
    }
}

dependencies {
    allModules.forEach { module ->
        aggregateJavadocClasspath(project(module))
        aggregateJavadocSourceDirs(project(module))
    }
    // The aggregate classpath lives in the root project, which has none of the modules' constraints.
    constraints {
        SimonVersions.MANAGED_COORDINATES.forEach { add(aggregateJavadocClasspath.name, it) }
    }
    // auto-service is compileOnly in annotation-processor (absent from the runtime classpath); add it
    // so Javadoc can resolve the @AutoService references.
    aggregateJavadocClasspath("com.google.auto.service:auto-service")
}

// Aggregated Javadoc parity with the maven-javadoc-plugin `aggregate` execution:
// output to <root>/documents, custom header/footer, -noqualifier all, non-fatal errors.
tasks.register<Javadoc>("aggregateJavadoc") {
    group = "documentation"
    description = "Generates aggregated Javadoc across all modules into documents/."
    setDestinationDir(file("documents"))
    isFailOnError = false
    source(aggregateJavadocSourceDirs.incoming.artifactView { lenient(true) }.files)
    classpath = aggregateJavadocClasspath
    (options as StandardJavadocDocletOptions).apply {
        header = "Simon - Reference Implementation"
        footer = "Simon - Reference Implementation"
        addStringOption("noqualifier", "all")
        encoding = "UTF-8"
    }
}
