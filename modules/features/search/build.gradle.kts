plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose.compiler)
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
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    api(libs.androidx.appcompat)
    api(libs.dagger.hilt.android)

    api(projects.modules.services.analytics)
    api(projects.modules.services.model)
    api(projects.modules.services.preferences)
    api(projects.modules.services.repositories)
    api(projects.modules.services.servers)
    api(projects.modules.services.ui)
    api(projects.modules.services.views)

    implementation(platform(libs.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.compose.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.reactive)
    implementation(libs.fragment.ktx)
    implementation(libs.lifecycle.reactivestreams.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.rx2.android)
    implementation(libs.rx2.java)
    implementation(libs.rx2.kotlin)
    implementation(libs.rx2.relay)
    implementation(libs.timber)

    implementation(projects.modules.services.compose)
    implementation(projects.modules.services.images)
    implementation(projects.modules.services.localization)
    implementation(projects.modules.services.utils)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)

    testImplementation(projects.modules.services.sharedtest)
}
