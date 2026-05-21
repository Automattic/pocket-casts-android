plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.pocketcasts.fingerprint"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(libs.jna) { artifact { type = "aar" } }
}
