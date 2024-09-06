plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.localization"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    compileOnly(libs.androidx.annotation)
}