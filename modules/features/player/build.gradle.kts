plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.player"
    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }
}

dependencies {
    api(libs.androidx.appcompat)
    implementation(libs.compose.animation)
    implementation(libs.compose.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.rx2)
    implementation(libs.fragment.ktx)
    implementation(libs.fragment.compose)
    api(libs.dagger.hilt.android)
    implementation(libs.lifecycle.reactivestreams.ktx)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    api(libs.lottie)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.extractor)
    api(libs.media3.ui)
    api(libs.okhttp)
    implementation(libs.rx2.android)
    api(libs.rx2.java)
    implementation(libs.rx2.relay)
    implementation(libs.rx2.kotlin)
    api(libs.androidx.cardview)
    implementation(libs.play.cast)
    api(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    api(libs.material)
    api(libs.material.progressbar)
    api(libs.androidx.mediarouter)
    implementation(libs.androidx.preference.ktx)
    api(libs.androidx.recyclerview)
    api(libs.showkase)
    implementation(libs.timber)
    api(libs.androidx.viewpager)
    implementation(platform(libs.compose.bom))

    testImplementation(libs.androidx.arch.core)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.turbine)
    testImplementation(libs.mockito.core)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)
    ksp(libs.showkase.processor)

    // features
    api(project(":modules:features:settings"))
    implementation(project(":modules:features:reimagine"))
    // services
    api(project(":modules:services:analytics"))
    api(project(":modules:services:compose"))
    implementation(project(":modules:services:images"))
    api(project(":modules:services:localization"))
    api(project(":modules:services:model"))
    api(project(":modules:services:preferences"))
    api(project(":modules:services:repositories"))
    api(project(":modules:services:servers"))
    api(project(":modules:services:utils"))
    api(project(":modules:services:ui"))
    api(project(":modules:services:views"))
    testImplementation(project(":modules:services:sharedtest"))
}
