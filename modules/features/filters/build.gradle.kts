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
    api(libs.androidx.appcompat)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.rx2)
    implementation(libs.fragment.ktx)
    implementation(libs.lifecycle.reactivestreams.ktx)
    api(libs.dagger.hilt.android)
    implementation(libs.rx2.android)
    api(libs.rx2.java)
    implementation(libs.rx2.kotlin)
    api(libs.androidx.cardview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    api(libs.flexbox)
    api(libs.material)
    implementation(libs.androidx.preference.ktx)
    api(libs.androidx.recyclerview)
    implementation(libs.timber)
    api(libs.androidx.viewpager)

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
