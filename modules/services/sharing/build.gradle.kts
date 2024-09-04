plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.sharing"
    buildFeatures {
        buildConfig = true
        viewBinding = false
        compose = false
    }
}

dependencies {
    implementation(libs.coroutines.core)
    api(libs.dagger.hilt.android)
    implementation(libs.coil)
    implementation(libs.timber)
    implementation(platform(libs.compose.bom))

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    api(projects.modules.services.model)
    api(projects.modules.services.analytics)
    implementation(projects.modules.services.images)
    implementation(projects.modules.services.localization)
    api(projects.modules.services.repositories)
    implementation(projects.modules.services.utils)
}
