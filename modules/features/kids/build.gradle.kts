plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.kids"
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(libs.compose.animation)
    implementation(libs.compose.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.fragment.ktx)
    api(libs.dagger.hilt.android)
    implementation(libs.androidx.core.ktx)
    api(libs.material)
    api(libs.showkase)
    implementation(platform(libs.compose.bom))

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockito.core)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)
    ksp(libs.showkase.processor)


    implementation(projects.modules.services.compose)
    implementation(projects.modules.services.localization)
    implementation(projects.modules.services.images)
    api(projects.modules.services.ui)
    api(projects.modules.services.repositories)
    api(projects.modules.services.analytics)

    testImplementation(projects.modules.services.sharedtest)
}