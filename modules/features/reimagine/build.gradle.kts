plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.reimagine"
    buildFeatures {
        buildConfig = true
        viewBinding = false
        compose = true
    }
}

dependencies {
    implementation(libs.compose.animation)
    implementation(libs.compose.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.ui.util)
    implementation(libs.coroutines.core)
    implementation(libs.fragment.ktx)
    api(libs.hilt.android)
    implementation(libs.media3.exoplayer)
    implementation(libs.core.ktx)
    api(libs.showkase)
    implementation(libs.timber)
    implementation(platform(libs.compose.bom))

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockito.core)
    testImplementation(libs.turbine)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)
    ksp(libs.moshi.kotlin.codegen)
    ksp(libs.showkase.processor)

    api(project(":modules:services:sharing"))
    api(project(":modules:services:analytics"))
    implementation(project(":modules:services:compose"))
    implementation(project(":modules:services:images"))
    implementation(project(":modules:services:localization"))
    api(project(":modules:services:model"))
    api(project(":modules:services:preferences"))
    api(project(":modules:services:repositories"))
    api(project(":modules:services:ui"))
    implementation(project(":modules:services:utils"))
    api(project(":modules:services:views"))

    implementation(libs.capturable)

    testImplementation(project(":modules:services:sharedtest"))
}
