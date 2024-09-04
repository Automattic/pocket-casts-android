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
    api(libs.rx2.java)
    api(libs.material)

    implementation(libs.rx2.extensions)
}
