plugins {
    id("simon.java-conventions")
}

dependencies {
    api(project(":compiler"))
    api(project(":compiler-backend"))
    api(project(":antlr-parser"))
    api("org.apache.commons:commons-lang3")
    api("org.slf4j:slf4j-api")
}
