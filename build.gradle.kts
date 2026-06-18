// Root Gradle build for Simon — a side-by-side alternative to the Maven build.
// Every module also has a pom.xml; this build reuses the same src/ trees and writes
// to build/ (Maven uses target/), so `mvn` and `gradle` can be used interchangeably.
//
// This file plays the role of the parent pom: it defines the shared version, the
// managed dependency versions (mirroring <properties> and <dependencyManagement>),
// the common Java/JUnit/publishing configuration, and the aggregated Javadoc task.

plugins {
    // Declared (not applied) here so module scripts can apply it without repeating the version.
    kotlin("jvm") version "2.3.21" apply false
}

// --- Managed dependency versions (mirrors parent pom <properties> + <dependencyManagement>) ---
val versions = mapOf(
    "junit"            to "6.1.0",
    "autoService"      to "1.1.1",
    "kotlin"           to "2.3.21",
    "slf4j"            to "2.0.18",
    "commonsLang3"     to "3.20.0",
    "commonsText"      to "1.15.0",
    "commonsIo"        to "2.22.0",
    "emfEcore"         to "2.42.0",
    "classgraph"       to "4.8.184",
    "streamex"         to "0.8.4",
    "antlr"            to "4.13.2",
    "guava"            to "33.6.0-jre",
    "compileTesting"   to "0.23.0",
)

// Coordinates pinned for all modules via dependency constraints, so module scripts
// declare versionless dependencies just like the poms do under <dependencyManagement>.
val managedCoordinates = listOf(
    "org.apache.commons:commons-lang3:${versions["commonsLang3"]}",
    "org.apache.commons:commons-text:${versions["commonsText"]}",
    "commons-io:commons-io:${versions["commonsIo"]}",
    "org.eclipse.emf:org.eclipse.emf.ecore:${versions["emfEcore"]}",
    "io.github.classgraph:classgraph:${versions["classgraph"]}",
    "one.util:streamex:${versions["streamex"]}",
    "com.google.auto.service:auto-service:${versions["autoService"]}",
    "org.jetbrains.kotlin:kotlin-stdlib:${versions["kotlin"]}",
    "org.jetbrains.kotlin:kotlin-reflect:${versions["kotlin"]}",
    "org.jetbrains.kotlin:kotlin-compiler-embeddable:${versions["kotlin"]}",
    "org.slf4j:slf4j-api:${versions["slf4j"]}",
    "org.slf4j:slf4j-simple:${versions["slf4j"]}",
    "org.junit.jupiter:junit-jupiter:${versions["junit"]}",
    "org.junit.platform:junit-platform-launcher:${versions["junit"]}",
    "org.antlr:antlr4:${versions["antlr"]}",
    "org.antlr:antlr4-runtime:${versions["antlr"]}",
    "com.google.guava:guava:${versions["guava"]}",
    "com.google.testing.compile:compile-testing:${versions["compileTesting"]}",
)

// Expose the version map to module scripts that need an explicit version (e.g. the antlr plugin).
extra["versions"] = versions

// The root project resolves the aggregate Javadoc classpath, so it needs repositories too.
repositories {
    mavenCentral()
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    group = "com.abstratt.simon"
    version = "0.0.4-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    // Jar / publication base name matches the Maven artifactId (dir "gen-utils" -> "simon-gen-utils").
    extensions.configure<BasePluginExtension> {
        archivesName.set("simon-${project.name}")
    }

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-Xlint:all,-processing")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    // Mirror parent pom <dependencyManagement>: pin versions on every relevant configuration so
    // modules can declare versionless coordinates. Constraints on `implementation` flow to the
    // compile/runtime/test classpaths (which extend it); `annotationProcessor` is independent.
    dependencies {
        managedCoordinates.forEach { coordinate ->
            constraints {
                add("implementation", coordinate)
                add("annotationProcessor", coordinate)
            }
        }
    }

    // Publishing parity with <distributionManagement> (GitHub Packages).
    extensions.configure<PublishingExtension> {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
                artifactId = "simon-${project.name}"
            }
        }
        repositories {
            maven {
                name = "github"
                url = uri("https://maven.pkg.github.com/abstratt/maven-releases")
                credentials {
                    username = System.getenv("GITHUB_ACTOR") ?: (findProperty("gpr.user") as String?)
                    password = System.getenv("GITHUB_TOKEN") ?: (findProperty("gpr.key") as String?)
                }
            }
        }
    }
}

// A root-owned resolvable configuration that pulls every module's runtime classpath. Aggregating
// through one configuration the root owns avoids resolving each subproject's own classpath during
// task execution (which Gradle forbids under parallel builds).
val aggregateJavadocClasspath: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    // Standard JVM runtime attributes so variant-aware dependencies (e.g. guava jre vs android)
    // resolve correctly; a plain configuration carries none of these by default.
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

// Pin the managed versions on the aggregate classpath too (it lives in the root project, which
// otherwise has none of the subproject constraints). auto-service is compileOnly in
// annotation-processor, so it is absent from the runtime-based aggregate classpath; add it
// explicitly so Javadoc can resolve the @AutoService references.
dependencies {
    constraints {
        managedCoordinates.forEach { add(aggregateJavadocClasspath.name, it) }
    }
    add(aggregateJavadocClasspath.name, "com.google.auto.service:auto-service")
}

// Aggregated Javadoc parity with the maven-javadoc-plugin `aggregate` execution:
// output to <root>/documents, custom header/footer, -noqualifier all, non-fatal errors.
tasks.register<Javadoc>("aggregateJavadoc") {
    group = "documentation"
    description = "Generates aggregated Javadoc across all modules into documents/."
    setDestinationDir(file("documents"))
    isFailOnError = false
    classpath = aggregateJavadocClasspath
    (options as StandardJavadocDocletOptions).apply {
        header = "Simon - Reference Implementation"
        footer = "Simon - Reference Implementation"
        addStringOption("noqualifier", "all")
        encoding = "UTF-8"
    }
}

// Subprojects are configured lazily; wire their main Java sources and the aggregate classpath
// once they are evaluated.
gradle.projectsEvaluated {
    val javaProjects = subprojects.filter { it.plugins.hasPlugin("java") }
    javaProjects.forEach { p -> dependencies.add(aggregateJavadocClasspath.name, p) }
    tasks.named<Javadoc>("aggregateJavadoc") {
        javaProjects.forEach { p ->
            source(p.extensions.getByType<SourceSetContainer>()["main"].allJava)
        }
    }
}
