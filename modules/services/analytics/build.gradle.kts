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
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    api(libs.dagger.hilt.android)

    api(projects.modules.services.model)
    api(projects.modules.services.preferences)
    api(projects.modules.services.utils)

    implementation(platform(libs.firebase.bom))

    implementation(libs.automattic.tracks)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.config.ktx)

    debugImplementation(libs.compose.ui.tooling)

    debugProdImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
}
