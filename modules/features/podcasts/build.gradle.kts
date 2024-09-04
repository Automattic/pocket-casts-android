plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.podcasts"
    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }
}

dependencies {
    api(libs.androidx.appcompat)
    implementation(libs.compose.animation)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.rx2)
    implementation(libs.fragment.ktx)
    api(libs.dagger.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.rx2.android)
    api(libs.rx2.java)
    api(libs.rx2.relay)
    implementation(libs.rx2.kotlin)
    api(libs.androidx.cardview)
    api(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    api(libs.material)
    implementation(libs.lifecycle.reactivestreams.ktx)
    api(libs.androidx.preference.ktx)
    api(libs.androidx.recyclerview)
    api(libs.androidx.swiperefreshlayout)
    implementation(libs.timber)
    api(libs.androidx.viewpager)
    implementation(platform(libs.compose.bom))
    implementation(platform(libs.firebase.bom))

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)
    ksp(libs.moshi.kotlin.codegen)

    // features
    implementation(project(":modules:features:account"))
    implementation(project(":modules:features:search"))
    implementation(project(":modules:features:reimagine"))
    implementation(project(":modules:features:settings"))
    api(project(":modules:features:player"))

    // services
    api(project(":modules:services:analytics"))
    api(project(":modules:services:compose"))
    implementation(project(":modules:services:images"))
    api(project(":modules:services:model"))
    api(project(":modules:services:localization"))
    api(project(":modules:services:preferences"))
    api(project(":modules:services:repositories"))
    api(project(":modules:services:servers"))
    api(project(":modules:services:utils"))
    api(project(":modules:services:ui"))
    api(project(":modules:services:views"))
}
