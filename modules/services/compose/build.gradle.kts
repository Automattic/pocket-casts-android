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
    implementation(libs.compose.material.icons)
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

    implementation(projects.modules.services.images)
    implementation(projects.modules.services.localization)
    api(projects.modules.services.model)
    api(projects.modules.services.preferences)
    api(projects.modules.services.repositories)
    api(projects.modules.services.ui)
    implementation(projects.modules.services.utils)
}
