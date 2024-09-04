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
    implementation(libs.material)
    implementation(libs.preference)
}
