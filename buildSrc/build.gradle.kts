plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    // Makes the Kotlin JVM plugin available to the simon.kotlin-conventions precompiled plugin.
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")
}
