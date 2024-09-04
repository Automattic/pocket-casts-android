plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.widget"
    buildFeatures {
        buildConfig = true
        viewBinding = false
        compose = true
    }
}

dependencies {
    api(libs.hilt.android)
    api(libs.moshi)
    implementation(libs.moshi.adapters)
    implementation(libs.coil)
    implementation(libs.timber)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)
    ksp(libs.moshi.kotlin.codegen)

    implementation(libs.compose.glance.appwidget)
    implementation(libs.compose.glance.material3)
    implementation(libs.compose.material3)
    api(project(":modules:services:analytics"))
    implementation(project(":modules:services:images"))
    implementation(project(":modules:services:localization"))
    api(project(":modules:services:model"))
    api(project(":modules:services:preferences"))
    api(project(":modules:services:repositories"))
}
