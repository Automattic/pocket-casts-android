plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

apply(from = "${project.rootDir}/base.gradle")

android {
    namespace = "au.com.shiftyjelly.pocketcasts.clip"
    buildFeatures {
        buildConfig = true
        viewBinding = false
        compose = true
    }
}

dependencies {
}
