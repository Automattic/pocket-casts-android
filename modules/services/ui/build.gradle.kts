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
    api(libs.androidx.appcompat)
    implementation(libs.browser.helper)
    implementation(libs.coroutines.core)
    api(libs.dagger.hilt.android)
    api(libs.hilt.work)
    implementation(libs.media3.cast)
    api(libs.work.runtime)
    implementation(libs.coil)
    implementation(libs.androidx.constraintlayout)
    api(libs.material)
    implementation(libs.material.dialogs)
    implementation(libs.material.progressbar)
    implementation(libs.androidx.mediarouter)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.timber)
    implementation(platform(libs.compose.bom))
    api(libs.compose.ui.graphics)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    implementation(projects.modules.services.images)
    implementation(projects.modules.services.localization)
    api(projects.modules.services.model)
    api(projects.modules.services.preferences)
    api(projects.modules.services.repositories)
    implementation(projects.modules.services.utils)
}
