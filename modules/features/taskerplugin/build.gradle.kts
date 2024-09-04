plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.taskerplugin"
    buildFeatures {
        buildConfig = true
        viewBinding = false
        compose = true
    }
}

dependencies {
    implementation(libs.compose.activity)
    implementation(libs.compose.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.ui.util)
    api(libs.dagger.hilt.android)
    implementation(platform(libs.compose.bom))

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)


    implementation(project(":modules:services:analytics"))
    implementation(project(":modules:services:localization"))
    api(project(":modules:services:ui"))
    implementation(project(":modules:services:compose"))
    api(project(":modules:services:repositories"))
    api(project(":modules:services:model"))
    implementation(project(":modules:services:images"))
    api(project(":modules:services:preferences"))
    implementation(project(":modules:services:utils"))
    api(libs.tasker)
}
