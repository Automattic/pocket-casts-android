plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.cartheme"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.preference.ktx)
    implementation(libs.material)
}
