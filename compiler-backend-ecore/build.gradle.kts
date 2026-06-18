plugins {
    id("simon.java-conventions")
}

dependencies {
    api(project(":gen-utils"))
    api(project(":compiler-backend"))
    api(project(":metamodel-ecore"))
}
