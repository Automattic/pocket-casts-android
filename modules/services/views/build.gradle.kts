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
    api(libs.androidx.appcompat)
    api(libs.compose.material)
    implementation(libs.compose.ui)
    implementation(libs.coroutines.core)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.fragment.ktx)
    api(libs.dagger.hilt.android)
    api(libs.lottie)
    implementation(libs.okhttp)
    implementation(libs.rx2.android)
    api(libs.rx2.java)
    api(libs.rx2.relay)
    implementation(libs.rx2.kotlin)
    implementation(libs.androidx.cardview)
    implementation(libs.play.cast)
    api(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    api(libs.material)
    api(libs.androidx.mediarouter)
    api(libs.androidx.preference.ktx)
    api(libs.androidx.recyclerview)
    implementation(libs.timber)
    api(libs.navigation.runtime)
    implementation(libs.lifecycle.reactivestreams.ktx)
    implementation(libs.androidx.viewpager)
    api(libs.play.review)
    implementation(platform(libs.compose.bom))
    api(libs.crashlogging)

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
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
