plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.referrals"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
}
