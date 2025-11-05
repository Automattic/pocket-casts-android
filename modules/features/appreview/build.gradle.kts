plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.appreview"
    buildFeatures {
        buildConfig = true
        viewBinding = false
        compose = true
    }
}
