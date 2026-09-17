plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    google()
}

dependencies {
    implementation(libs.gradle.agp)
    implementation(libs.gradle.kotlin) {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-compiler-embeddable")
    }
    implementation(libs.gradle.serialization)
    implementation(libs.gradle.kotlinter)
}
