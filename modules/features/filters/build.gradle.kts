plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.filters"
    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = false
    }
}

dependencies {
    api(libs.appcompat)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.rx2)
    implementation(libs.fragment.ktx)
    implementation(libs.lifecycle.reactivestreams)
    api(libs.hilt.android)
    implementation(libs.rxandroid)
    api(libs.rxjava)
    implementation(libs.rxkotlin)
    api(libs.cardview)
    implementation(libs.constraintlayout)
    implementation(libs.core.ktx)
    api(libs.flexbox)
    api(libs.material)
    implementation(libs.preference.ktx)
    api(libs.recyclerview)
    implementation(libs.timber)
    api(libs.viewpager)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    // features
    api(project(":modules:features:podcasts"))

    // services
    api(project(":modules:services:analytics"))
    implementation(project(":modules:services:images"))
    implementation(project(":modules:services:localization"))
    api(project(":modules:services:model"))
    api(project(":modules:services:preferences"))
    api(project(":modules:services:repositories"))
    implementation(project(":modules:services:utils"))
    api(project(":modules:services:ui"))
    api(project(":modules:services:views"))
}
