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
    implementation("androidx.compose.ui:ui-graphics:1.6.2")
    api("androidx.compose.ui:ui-unit:1.6.2")
    implementation(platform(libs.compose.bom))
    api(libs.compose.ui)
    implementation(libs.material)
}