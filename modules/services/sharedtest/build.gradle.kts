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
    api(libs.automattic.crashlogging)
    api(libs.junit)

    implementation(libs.coroutines.test)
    implementation(libs.rx2.android)

    implementation(projects.modules.services.utils)
}
