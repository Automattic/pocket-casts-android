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
    api(libs.compose.ui)

    implementation(platform(libs.compose.bom))

    implementation(libs.compose.ui.graphics)
    implementation(libs.material)
}