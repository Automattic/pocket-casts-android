plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose.compiler)
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
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    api(libs.dagger.hilt.android)
    api(libs.tasker)

    api(projects.modules.services.model)
    api(projects.modules.services.preferences)
    api(projects.modules.services.repositories)
    api(projects.modules.services.ui)

    implementation(platform(libs.compose.bom))

    implementation(libs.compose.activity)
    implementation(libs.compose.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.ui.util)

    implementation(projects.modules.services.analytics)
    implementation(projects.modules.services.compose)
    implementation(projects.modules.services.images)
    implementation(projects.modules.services.localization)
    implementation(projects.modules.services.utils)
}
