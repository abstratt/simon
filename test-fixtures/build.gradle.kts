// Despite the name, this is a normal main-scope library of shared test helpers (it is consumed
// by the `tests` module in test scope). It is deliberately NOT the java-test-fixtures plugin —
// the Maven module ships these as ordinary main classes.
dependencies {
    api(project(":annotation-dsl"))
    api(project(":example-languages"))
    api(project(":example-languages-kotlin"))
    api(project(":antlr-compiler"))
    api(project(":compiler-source-ecore"))
    api(project(":compiler-source-annotated-java"))
    api(project(":compiler-backend-ecore"))
    api(project(":compiler-source-simon"))
    api(project(":annotation-processor"))

    api("org.junit.jupiter:junit-jupiter")
    api("com.google.guava:guava")
    api("org.jetbrains.kotlin:kotlin-stdlib")
    api("com.google.testing.compile:compile-testing") {
        // compile-testing pulls in JUnit 4; exclude it so only JUnit 5 is on the classpath.
        exclude(group = "junit")
    }
}
