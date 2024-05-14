plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

apply(from = "${project.rootDir}/base.gradle")

android {
    namespace = "au.com.shiftyjelly.pocketcasts.nova"
    buildFeatures {
        buildConfig = true
        viewBinding = false
        compose = false
    }
}

dependencies {
    // AAR dependencies cannot be resolved with Version Catalogs https://github.com/gradle/gradle/issues/20074
    implementation("io.branch.engage:conduit-source:0.2.3-pocketcasts.4@aar") { isTransitive = true }
}
