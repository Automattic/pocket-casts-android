plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.images"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.compose.ui.graphics)
    implementation(platform(libs.compose.bom))
    api(libs.compose.ui)
    implementation(libs.material)
}