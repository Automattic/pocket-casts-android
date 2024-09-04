plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.kids"
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(libs.compose.animation)
    implementation(libs.compose.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.fragment.ktx)
    api(libs.dagger.hilt.android)
    implementation(libs.androidx.core.ktx)
    api(libs.material)
    api(libs.showkase)
    implementation(platform(libs.compose.bom))

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockito.core)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)
    ksp(libs.showkase.processor)


    implementation(project(":modules:services:compose"))
    implementation(project(":modules:services:localization"))
    implementation(project(":modules:services:images"))
    api(project(":modules:services:ui"))
    api(project(":modules:services:repositories"))
    api(project(":modules:services:analytics"))

    testImplementation(project(":modules:services:sharedtest"))
}