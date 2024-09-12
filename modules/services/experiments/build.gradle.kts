plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.pocketcasts.experiments"
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

    api(projects.modules.services.repositories)

    implementation(projects.modules.services.utils)
    implementation(libs.automattic.explat)
}
