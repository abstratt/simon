pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "simon"

// The active modules — mirrors the <modules> list in the parent pom.xml.
// kotlin-compiler-plugin, lsp-server and tests-kt are intentionally excluded
// (they are commented out in pom.xml).
include(
    "gen-utils",
    "metamodel",
    "annotation-dsl",
    "annotation-processor",
    "compiler-source",
    "compiler-backend",
    "compiler",
    "antlr-parser",
    "antlr-compiler",
    "metamodel-ecore",
    "compiler-source-ecore",
    "compiler-source-annotated-java",
    "compiler-backend-ecore",
    "compiler-source-simon",
    "example-languages",
    "example-languages-kotlin",
    "test-fixtures",
    "tests",
)
