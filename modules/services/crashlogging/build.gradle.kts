plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

apply(from = "${project.rootDir}/base.gradle")

android {
    namespace = "au.com.shiftyjelly.pocketcasts.crashlogging"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":modules:services:utils")) //temporarily
    implementation("com.automattic.tracks:crashlogging:5.0.0")
}
