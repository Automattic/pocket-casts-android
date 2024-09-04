plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.search"
    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }
}

dependencies {
    api(libs.androidx.appcompat)
    implementation(libs.coroutines.reactive)
    implementation(libs.compose.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.coroutines.core)
    implementation(libs.fragment.ktx)
    api(libs.dagger.hilt.android)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.rx2.android)
    implementation(libs.rx2.java)
    implementation(libs.rx2.relay)
    implementation(libs.rx2.kotlin)
    implementation(libs.androidx.core.ktx)
    implementation(libs.lifecycle.reactivestreams.ktx)
    implementation(libs.timber)
    implementation(platform(libs.compose.bom))

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockito.core)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    api(projects.modules.services.analytics)
    implementation(projects.modules.services.localization)
    api(projects.modules.services.preferences)
    implementation(projects.modules.services.utils)
    implementation(projects.modules.services.images)
    api(projects.modules.services.ui)
    implementation(projects.modules.services.compose)
    api(projects.modules.services.views)
    api(projects.modules.services.servers)
    api(projects.modules.services.repositories)
    api(projects.modules.services.model)
    testImplementation(projects.modules.services.sharedtest)
}
