plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.engage"
    buildFeatures {
        buildConfig = true
        viewBinding = false
        compose = false
    }
}

dependencies {
    api(libs.dagger.hilt.android)
    api(libs.hilt.work)
    implementation(libs.coroutines.play.services)
    implementation(libs.lifecycle.process)
    implementation(libs.work.runtime)
    implementation(libs.timber)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)

    implementation(projects.modules.services.analytics)
    implementation(projects.modules.services.deeplink)
    implementation(projects.modules.services.localization)
    implementation(projects.modules.services.model)
    api(projects.modules.services.repositories)
    implementation(projects.modules.services.utils)

    implementation(libs.engage)
}
