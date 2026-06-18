plugins {
    id("simon.java-conventions")
}

dependencies {
    testImplementation(project(":test-fixtures"))
    testRuntimeOnly("org.slf4j:slf4j-simple")
    // Gradle 9 no longer puts the JUnit Platform launcher on the test classpath automatically.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
