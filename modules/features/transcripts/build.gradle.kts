plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.transcripts"
    buildFeatures {
        buildConfig = true
        viewBinding = false
        compose = true
    }
}

dependencies {
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    api(libs.compose.runtime)
    api(libs.dagger.hilt.android)

    api(projects.modules.services.analytics)
    api(projects.modules.services.payment)
    api(projects.modules.services.repositories)
    api(projects.modules.services.sharing)
    api(projects.modules.services.views)

    implementation(platform(libs.compose.bom))

    implementation(libs.androidx.webkit)
    implementation(libs.compose.material)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.webview)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.reactive)
    implementation(libs.coroutines.rx2)
    implementation(libs.rx2.java)

    implementation(projects.modules.features.settings)
    implementation(projects.modules.services.compose)
    implementation(projects.modules.services.images)
    implementation(projects.modules.services.localization)
    implementation(projects.modules.services.ui)
    implementation(projects.modules.services.utils)
    implementation(projects.modules.services.model)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.turbine)

    testImplementation(projects.modules.services.sharedtest)
}
