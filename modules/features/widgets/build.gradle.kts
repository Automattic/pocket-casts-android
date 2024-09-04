plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.widget"
    buildFeatures {
        buildConfig = true
        viewBinding = false
        compose = true
    }
}

dependencies {
    api(libs.dagger.hilt.android)
    api(libs.moshi)
    implementation(libs.moshi.adapters)
    implementation(libs.coil)
    implementation(libs.timber)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)
    ksp(libs.moshi.kotlin.codegen)

    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    implementation(libs.compose.material3)
    api(projects.modules.services.analytics)
    implementation(projects.modules.services.images)
    implementation(projects.modules.services.localization)
    api(projects.modules.services.model)
    api(projects.modules.services.preferences)
    api(projects.modules.services.repositories)
}
