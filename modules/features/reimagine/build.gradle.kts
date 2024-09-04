plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.reimagine"
    buildFeatures {
        buildConfig = true
        viewBinding = false
        compose = true
    }
}

dependencies {
    implementation(libs.compose.animation)
    implementation(libs.compose.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.ui.util)
    implementation(libs.coroutines.core)
    implementation(libs.fragment.ktx)
    api(libs.dagger.hilt.android)
    implementation(libs.media3.exoplayer)
    implementation(libs.androidx.core.ktx)
    api(libs.showkase)
    implementation(libs.timber)
    implementation(platform(libs.compose.bom))

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockito.core)
    testImplementation(libs.turbine)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)
    ksp(libs.moshi.kotlin.codegen)
    ksp(libs.showkase.processor)

    api(projects.modules.services.sharing)
    api(projects.modules.services.analytics)
    implementation(projects.modules.services.compose)
    implementation(projects.modules.services.images)
    implementation(projects.modules.services.localization)
    api(projects.modules.services.model)
    api(projects.modules.services.preferences)
    api(projects.modules.services.repositories)
    api(projects.modules.services.ui)
    implementation(projects.modules.services.utils)
    api(projects.modules.services.views)

    implementation(libs.capturable)

    testImplementation(projects.modules.services.sharedtest)
}
