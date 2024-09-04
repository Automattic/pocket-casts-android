plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.search"
    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }
}

dependencies {
    api(libs.androidx.appcompat)
    implementation(libs.coroutines.reactive)
    implementation(libs.compose.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.coroutines.core)
    implementation(libs.fragment.ktx)
    api(libs.dagger.hilt.android)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.rx2.android)
    implementation(libs.rx2.java)
    implementation(libs.rx2.relay)
    implementation(libs.rx2.kotlin)
    implementation(libs.androidx.core.ktx)
    implementation(libs.lifecycle.reactivestreams.ktx)
    implementation(libs.timber)
    implementation(platform(libs.compose.bom))

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockito.core)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    api(project(":modules:services:analytics"))
    implementation(project(":modules:services:localization"))
    api(project(":modules:services:preferences"))
    implementation(project(":modules:services:utils"))
    implementation(project(":modules:services:images"))
    api(project(":modules:services:ui"))
    implementation(project(":modules:services:compose"))
    api(project(":modules:services:views"))
    api(project(":modules:services:servers"))
    api(project(":modules:services:repositories"))
    api(project(":modules:services:model"))
    testImplementation(project(":modules:services:sharedtest"))
}
