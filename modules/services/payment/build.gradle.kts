plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.payment"

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(libs.billing.ktx)
    api(libs.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
