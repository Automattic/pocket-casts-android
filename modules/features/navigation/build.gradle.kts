plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.navigation"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(libs.material)
    api(libs.rx2.java)

    implementation(platform(libs.compose.bom))
    implementation(libs.rx2.extensions)
    implementation(projects.modules.services.views)
}
