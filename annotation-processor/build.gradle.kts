dependencies {
    api(project(":annotation-dsl"))
    api("org.slf4j:slf4j-api")

    // auto-service runs as an annotation processor here to generate the
    // META-INF/services/javax.annotation.processing.Processor descriptor
    // for SimonDSLProcessor (Maven: <scope>provided</scope>).
    compileOnly("com.google.auto.service:auto-service")
    annotationProcessor("com.google.auto.service:auto-service")
}
