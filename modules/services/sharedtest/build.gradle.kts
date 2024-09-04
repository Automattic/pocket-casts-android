plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.sharedtest"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.rx2.android)


    implementation(libs.coroutines.test)
    implementation(project(":modules:services:utils"))

    api(libs.crashlogging)
    api(libs.junit)
}
