plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.parcelize)
}

apply(from = "${project.rootDir}/base.gradle")

android {
    namespace = "au.com.shiftyjelly.pocketcasts.sharing"
    buildFeatures {
        buildConfig = true
        viewBinding = false
        compose = true
    }
}

dependencies {
    val debugProdImplementation by configurations

    implementation(libs.capturable)

    implementation(project(":modules:services:analytics"))
    implementation(project(":modules:services:compose"))
    implementation(project(":modules:services:images"))
    implementation(project(":modules:services:localization"))
    implementation(project(":modules:services:model"))
    implementation(project(":modules:services:preferences"))
    implementation(project(":modules:services:repositories"))
    implementation(project(":modules:services:ui"))
    implementation(project(":modules:services:utils"))
    implementation(project(":modules:services:views"))

    // We do not include FFmpeg in the release variant for now
    // to not increase the binary size.
    // We can add it once we release clip sharing.
    debugImplementation(libs.ffmpeg)
    debugProdImplementation(libs.ffmpeg)

    testImplementation(project(":modules:services:sharedtest"))
}
