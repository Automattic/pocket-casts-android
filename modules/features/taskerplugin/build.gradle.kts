plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.taskerplugin"
    buildFeatures {
        buildConfig = true
        viewBinding = false
        compose = true
    }
}

dependencies {
    implementation(libs.compose.activity)
    implementation(libs.compose.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.ui.util)
    api(libs.dagger.hilt.android)
    implementation(platform(libs.compose.bom))

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)


    implementation(projects.modules.services.analytics)
    implementation(projects.modules.services.localization)
    api(projects.modules.services.ui)
    implementation(projects.modules.services.compose)
    api(projects.modules.services.repositories)
    api(projects.modules.services.model)
    implementation(projects.modules.services.images)
    api(projects.modules.services.preferences)
    implementation(projects.modules.services.utils)
    api(libs.tasker)
}
