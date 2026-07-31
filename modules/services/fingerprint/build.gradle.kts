plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.pocketcasts.fingerprint"
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        consumerProguardFiles("proguard-rules.pro")
    }
}

dependencies {
    api(libs.jna) { artifact { type = "aar" } }
}
