plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

apply(from = "${project.rootDir}/base.gradle")

android {
    namespace = "au.com.shiftyjelly.pocketcasts.engage"
    buildFeatures {
        buildConfig = true
        viewBinding = false
        compose = false
    }
}

dependencies {
    implementation(project(":modules:services:analytics"))
    implementation(project(":modules:services:deeplink"))
    implementation(project(":modules:services:localization"))
    implementation(project(":modules:services:model"))
    implementation(project(":modules:services:repositories"))
    implementation(project(":modules:services:utils"))

    implementation(libs.engage)
}
