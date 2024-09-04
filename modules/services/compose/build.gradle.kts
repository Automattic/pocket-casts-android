plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.compose"
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(libs.compose.activity)
    implementation(libs.compose.animation)
    implementation(libs.compose.icons)
    implementation(libs.compose.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.ui.util)
    implementation(libs.lottie)
    implementation(libs.lottie.compose)
    implementation(libs.coil.compose)
    api(libs.showkase)
    implementation(platform(libs.compose.bom))

    ksp(libs.showkase.processor)

    implementation(project(":modules:services:images"))
    implementation(project(":modules:services:localization"))
    api(project(":modules:services:model"))
    api(project(":modules:services:preferences"))
    api(project(":modules:services:repositories"))
    api(project(":modules:services:ui"))
    implementation(project(":modules:services:utils"))
}
