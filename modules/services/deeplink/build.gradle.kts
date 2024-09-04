plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
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
    implementation(libs.core)
    implementation(libs.timber)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit) { exclude(group = "org.hamcrest") }
}
