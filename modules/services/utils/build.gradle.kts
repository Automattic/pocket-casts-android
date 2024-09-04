import org.jetbrains.kotlin.utils.addToStdlib.butIf

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.parcelize)
}

// utils should have no other module dependencies

android {
    namespace = "au.com.shiftyjelly.pocketcasts.helper"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(libs.coil.base)
    implementation(libs.lifecycle.livedata)
    testImplementation(libs.mockito.core)

    api(libs.appcompat)
    implementation(libs.auth)
    implementation(libs.coroutines.core)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.config)
    api(libs.hilt.android)
    api(libs.okhttp)
    implementation(libs.rxandroid)
    api(libs.rxjava)
    implementation(libs.car)
    implementation(libs.annotation)
    implementation(libs.cast)
    implementation(libs.compose.ui.graphics)
    implementation(libs.core.ktx)
    api(libs.material)
    api(libs.timber)
    implementation(platform(libs.compose.bom))
    implementation(platform(libs.firebase.bom))

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.kotlin)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)
}
