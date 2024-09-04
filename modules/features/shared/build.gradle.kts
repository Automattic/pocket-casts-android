plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.shared"
    buildFeatures {
        buildConfig = true
        viewBinding = false
        compose = false
    }
}

dependencies {
    implementation(libs.coroutines.play.services)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.config.ktx)
    api(libs.dagger.hilt.android)
    implementation(libs.lifecycle.process)
    implementation(libs.play.wearable)
    implementation(libs.timber)
    implementation(platform(libs.firebase.bom))

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.lifecycle.runtime.testing)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockito.core)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    api(projects.modules.services.analytics)
    api(projects.modules.services.crashlogging)
    api(projects.modules.services.preferences)
    api(projects.modules.services.repositories)
    api(projects.modules.services.utils)
    api(projects.modules.services.model)
}
