import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType

// Common configuration applied by every module — the Isolated-Projects-safe replacement for the
// root subprojects {} block (each module applies this plugin directly).
plugins {
    `java-library`
    `maven-publish`
}

group = "com.abstratt.simon"
version = "0.0.4-SNAPSHOT"

repositories {
    mavenCentral()
}

// Jar / publication base name matches the Maven artifactId (dir "gen-utils" -> "simon-gen-utils").
base {
    archivesName.set("simon-${project.name}")
}

java {
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

// Mirror parent pom <dependencyManagement>: pin versions on every relevant configuration so modules
// declare versionless coordinates. Constraints on `implementation` flow to the compile/runtime/test
// classpaths (which extend it); `annotationProcessor` is independent.
dependencies {
    constraints {
        SimonVersions.MANAGED_COORDINATES.forEach { coordinate ->
            add("implementation", coordinate)
            add("annotationProcessor", coordinate)
        }
    }
}

// Publishing parity with <distributionManagement> (GitHub Packages).
publishing {
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
                // providers.* is project-scoped and Isolated-Projects/configuration-cache safe;
                // findProperty would fall back to a dynamic lookup in the parent project.
                username = providers.environmentVariable("GITHUB_ACTOR")
                    .orElse(providers.gradleProperty("gpr.user")).orNull
                password = providers.environmentVariable("GITHUB_TOKEN")
                    .orElse(providers.gradleProperty("gpr.key")).orNull
            }
        }
    }
}

// Expose this module's hand-written main Java source directory as a consumable artifact, so the
// root project can aggregate Javadoc without reaching into other projects' models (IP-safe).
val javadocSourceDirs = configurations.create("javadocSourceDirs") {
    isCanBeResolved = false
    isCanBeConsumed = true
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
        attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named("javadoc-source-dirs"))
    }
}
val mainJavaDir = layout.projectDirectory.dir("src/main/java")
if (mainJavaDir.asFile.exists()) {
    artifacts.add(javadocSourceDirs.name, mainJavaDir.asFile)
}
