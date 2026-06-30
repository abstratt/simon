/**
 * Single source of truth for dependency versions, shared by the convention plugins and any module
 * that needs an explicit version (e.g. the ANTLR tool). Mirrors the parent pom <properties> and
 * <dependencyManagement>.
 */
object SimonVersions {
    const val JUNIT = "6.1.1"
    const val AUTO_SERVICE = "1.1.1"
    const val KOTLIN = "2.4.0"
    const val SLF4J = "2.0.18"
    const val COMMONS_LANG3 = "3.20.0"
    const val COMMONS_TEXT = "1.15.0"
    const val COMMONS_IO = "2.22.0"
    const val EMF_ECORE = "2.42.0"
    const val CLASSGRAPH = "4.8.184"
    const val STREAMEX = "0.8.4"
    const val ANTLR = "4.13.2"
    const val GUAVA = "33.6.0-jre"
    const val COMPILE_TESTING = "0.23.0"

    /** Coordinates pinned for all modules via dependency constraints (Maven <dependencyManagement>). */
    val MANAGED_COORDINATES = listOf(
        "org.apache.commons:commons-lang3:$COMMONS_LANG3",
        "org.apache.commons:commons-text:$COMMONS_TEXT",
        "commons-io:commons-io:$COMMONS_IO",
        "org.eclipse.emf:org.eclipse.emf.ecore:$EMF_ECORE",
        "io.github.classgraph:classgraph:$CLASSGRAPH",
        "one.util:streamex:$STREAMEX",
        "com.google.auto.service:auto-service:$AUTO_SERVICE",
        "org.jetbrains.kotlin:kotlin-stdlib:$KOTLIN",
        "org.jetbrains.kotlin:kotlin-reflect:$KOTLIN",
        "org.jetbrains.kotlin:kotlin-compiler-embeddable:$KOTLIN",
        "org.slf4j:slf4j-api:$SLF4J",
        "org.slf4j:slf4j-simple:$SLF4J",
        "org.junit.jupiter:junit-jupiter:$JUNIT",
        "org.junit.platform:junit-platform-launcher:$JUNIT",
        "org.antlr:antlr4:$ANTLR",
        "org.antlr:antlr4-runtime:$ANTLR",
        "com.google.guava:guava:$GUAVA",
        "com.google.testing.compile:compile-testing:$COMPILE_TESTING",
    )
}
