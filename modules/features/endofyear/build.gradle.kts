plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.endofyear"
    buildFeatures {
        buildConfig = true
        viewBinding = false
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.compose.activity)
    implementation(libs.compose.material)
    implementation(libs.compose.constraintlayout)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.coroutines.core)
    api(libs.dagger.hilt.android)
    implementation(libs.androidx.core.ktx)
    api(libs.showkase)
    implementation(libs.timber)
    api(libs.crashlogging)
    implementation(platform(libs.compose.bom))

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockito.core)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)
    ksp(libs.showkase.processor)

    // features
    implementation(projects.modules.features.settings)
    // services
    api(projects.modules.services.analytics)
    implementation(projects.modules.services.compose)
    implementation(projects.modules.services.images)
    implementation(projects.modules.services.localization)
    api(projects.modules.services.model)
    api(projects.modules.services.preferences)
    api(projects.modules.services.repositories)
    api(projects.modules.services.servers)
    api(projects.modules.services.ui)
    api(projects.modules.services.utils)
    api(projects.modules.services.views)
    testImplementation(projects.modules.services.sharedtest)
}
