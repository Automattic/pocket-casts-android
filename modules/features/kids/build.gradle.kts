plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose.compiler)
}

apply(from = "${project.rootDir}/base.gradle")

android {
    namespace = "au.com.shiftyjelly.pocketcasts.kids"
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(project(":modules:services:compose"))
    implementation(project(":modules:services:localization"))
    implementation(project(":modules:services:images"))
    implementation(project(":modules:services:ui"))
    implementation(project(":modules:services:repositories"))
    implementation(project(":modules:services:servers"))
    implementation(project(":modules:services:analytics"))

    testImplementation(project(":modules:services:sharedtest"))
}