plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.appfunctions"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    ksp(libs.androidx.appfunctions.compiler)
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    api(libs.androidx.appfunctions)
    api(libs.dagger.hilt.android)

    api(projects.modules.services.model)
    api(projects.modules.services.repositories)

    implementation(libs.coroutines.core)

    implementation(projects.modules.services.analytics)
    implementation(projects.modules.services.localization)
    implementation(projects.modules.services.preferences)
    implementation(projects.modules.services.servers)
    implementation(projects.modules.services.utils)
}
