plugins {
    id("simon.java-conventions")
}

dependencies {
    api(project(":annotation-dsl"))
    api("org.apache.commons:commons-text")

    // Run the Simon DSL annotation processor during compilation (Maven: <scope>provided</scope>).
    annotationProcessor(project(":annotation-processor"))
}
