plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.navigation"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(libs.rxjava)
    api(libs.material)

    implementation("com.github.akarnokd:rxjava2-extensions:0.20.10")
}
