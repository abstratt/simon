plugins {
    antlr
}

@Suppress("UNCHECKED_CAST")
val versions = rootProject.extra["versions"] as Map<String, String>

dependencies {
    // The `antlr` configuration is independent of `implementation`, so it isn't covered by the
    // managed-version constraints; pin the antlr tool version from the central map directly.
    antlr("org.antlr:antlr4:${versions["antlr"]}")
    api("org.antlr:antlr4-runtime")
}

// The grammar lives in src/main/antlr4 (the antlr4-maven-plugin convention); Gradle's antlr
// plugin defaults to src/main/antlr, so repoint it.
sourceSets {
    main {
        antlr {
            setSrcDirs(listOf("src/main/antlr4"))
        }
    }
}

// The grammar lives under src/main/antlr4/com/abstratt/simon/parser/antlr/, but ANTLR does not
// derive the Java package from the directory the way the antlr4-maven-plugin does, so set it
// explicitly to match the package the antlr-compiler module imports.
tasks.withType<org.gradle.api.plugins.antlr.AntlrTask>().configureEach {
    arguments = arguments + listOf("-package", "com.abstratt.simon.parser.antlr")
}
