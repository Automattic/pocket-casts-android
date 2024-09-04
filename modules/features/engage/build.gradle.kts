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
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    api(libs.dagger.hilt.android)
    api(libs.hilt.work)

    api(projects.modules.services.repositories)

    implementation(libs.coroutines.play.services)
    implementation(libs.engage)
    implementation(libs.lifecycle.process)
    implementation(libs.timber)
    implementation(libs.work.runtime)

    implementation(projects.modules.services.analytics)
    implementation(projects.modules.services.deeplink)
    implementation(projects.modules.services.localization)
    implementation(projects.modules.services.model)
    implementation(projects.modules.services.utils)

    testImplementation(libs.junit)
}
