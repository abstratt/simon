plugins {
    id("simon.java-conventions")
}

dependencies {
    api(project(":compiler-source"))
    api(project(":compiler-source-annotated-java"))
    api(project(":antlr-compiler"))
    api(project(":compiler-backend-ecore"))
}
