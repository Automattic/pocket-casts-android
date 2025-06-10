plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.transcripts"
    buildFeatures {
        buildConfig = true
        viewBinding = false
        compose = true
    }
}

dependencies {
    api(libs.compose.runtime)
    api(libs.okhttp)

    api(projects.modules.services.compose)

    implementation(platform(libs.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.compose.material)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.media3.common)
    implementation(libs.media3.extractor)
    implementation(libs.moshi)

    implementation(projects.modules.services.ui)
    implementation(projects.modules.services.model)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
