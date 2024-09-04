plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.analytics"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(libs.hilt.android)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.config)

    debugImplementation(libs.compose.ui.tooling)

    debugProdImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit) { exclude(group = "org.hamcrest") }

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    implementation(libs.tracks)
    api(project(":modules:services:utils"))
    api(project(":modules:services:preferences"))
    api(project(":modules:services:model"))
}
