plugins {
    alias(libs.plugins.android.library)
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
