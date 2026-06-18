plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":annotation-dsl"))
    api(project(":example-languages"))
    api("org.jetbrains.kotlin:kotlin-reflect")
    api("org.apache.commons:commons-text")

    // The Simon DSL processor is a Java annotation processor; it does not run over Kotlin
    // sources, but is declared for parity with the Maven `provided` dependency.
    annotationProcessor(project(":annotation-processor"))
}
