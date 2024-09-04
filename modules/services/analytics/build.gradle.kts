plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.analytics"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(libs.dagger.hilt.android)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.config.ktx)

    debugImplementation(libs.compose.ui.tooling)

    debugProdImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    implementation(libs.tracks)
    api(projects.modules.services.utils)
    api(projects.modules.services.preferences)
    api(projects.modules.services.model)
}
