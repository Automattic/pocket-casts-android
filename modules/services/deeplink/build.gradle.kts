plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.deeplink"
    buildFeatures {
        buildConfig = true
        viewBinding = false
        compose = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.timber)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
}
