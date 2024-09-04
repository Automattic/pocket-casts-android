plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.views"
    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }
}

dependencies {
    api(libs.appcompat)
    api(libs.compose.material)
    implementation(libs.compose.ui)
    implementation(libs.coroutines.core)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.fragment.ktx)
    api(libs.hilt.android)
    api(libs.lottie)
    implementation(libs.okhttp)
    implementation(libs.rxandroid)
    api(libs.rxjava)
    api(libs.rxrelay)
    implementation(libs.rxkotlin)
    implementation(libs.cardview)
    implementation(libs.cast)
    api(libs.constraintlayout)
    implementation(libs.core.ktx)
    api(libs.material)
    api(libs.mediarouter)
    api(libs.preference)
    api(libs.recyclerview)
    implementation(libs.timber)
    api(libs.navigation.runtime)
    implementation(libs.lifecycle.reactivestreams.java)
    implementation(libs.viewpager)
    api("com.google.android.play:review:2.0.1")
    implementation(platform(libs.compose.bom))
    api(libs.crashlogging)

    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation(libs.mockito.kotlin)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)


    api(project(":modules:services:analytics"))
    api(project(":modules:services:compose"))
    implementation(project(":modules:services:images"))
    implementation(project(":modules:services:localization"))
    api(project(":modules:services:model"))
    api(project(":modules:services:preferences"))
    api(project(":modules:services:repositories"))
    api(project(":modules:services:servers"))
    api(project(":modules:services:ui"))
    api(project(":modules:services:utils"))
}
