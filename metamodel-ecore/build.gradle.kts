plugins {
    id("simon.java-conventions")
}

dependencies {
    api(project(":annotation-dsl"))
    api("org.apache.commons:commons-text")
    api("commons-io:commons-io")
    api("org.eclipse.emf:org.eclipse.emf.ecore")
    api("org.slf4j:slf4j-api")
}
