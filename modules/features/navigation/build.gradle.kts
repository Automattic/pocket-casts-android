plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.navigation"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(libs.material)
    api(libs.rx2.java)

    implementation(libs.rx2.extensions)
}
