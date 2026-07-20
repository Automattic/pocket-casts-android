plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.qr"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))

    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.androidx.core.ktx)
    implementation(libs.coroutines.core)
    implementation(libs.timber)
    implementation(libs.zxing)
}
