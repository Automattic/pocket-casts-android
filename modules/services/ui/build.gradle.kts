plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.ui"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    api(libs.androidx.appcompat)
    api(libs.compose.ui.graphics)
    api(libs.dagger.hilt.android)
    api(libs.hilt.work)
    api(libs.material)
    api(libs.work.runtime)

    api(projects.modules.services.model)
    api(projects.modules.services.preferences)
    api(projects.modules.services.repositories)

    implementation(platform(libs.compose.bom))

    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.mediarouter)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.browser.helper)
    implementation(libs.coil)
    implementation(libs.coroutines.core)
    implementation(libs.material.dialogs)
    implementation(libs.material.progressbar)
    implementation(libs.media3.cast)
    implementation(libs.timber)

    implementation(projects.modules.services.images)
    implementation(projects.modules.services.localization)
    implementation(projects.modules.services.utils)
}
