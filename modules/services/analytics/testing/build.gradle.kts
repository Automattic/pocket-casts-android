plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.analytics.testing"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(libs.eventhorizon)
}
