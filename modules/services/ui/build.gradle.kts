plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.ui"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(libs.appcompat)
    implementation(libs.browser.helper)
    implementation(libs.coroutines.core)
    api(libs.hilt.android)
    api(libs.hilt.work)
    implementation(libs.media3.cast)
    api(libs.work.runtime)
    implementation(libs.coil)
    implementation(libs.constraintlayout)
    api(libs.material)
    implementation(libs.material.dialogs)
    implementation(libs.material.progressbar)
    implementation(libs.mediarouter)
    implementation(libs.preference.ktx)
    implementation(libs.timber)
    implementation(platform(libs.compose.bom))
    api(libs.compose.ui.graphics)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    implementation(project(":modules:services:images"))
    implementation(project(":modules:services:localization"))
    api(project(":modules:services:model"))
    api(project(":modules:services:preferences"))
    api(project(":modules:services:repositories"))
    implementation(project(":modules:services:utils"))
}
