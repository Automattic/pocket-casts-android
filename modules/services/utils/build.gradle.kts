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
    api("io.coil-kt:coil-base:2.5.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.8.4")
    testImplementation("org.mockito:mockito-core:5.7.0")

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
    implementation("androidx.compose.ui:ui-graphics:1.6.2")
    implementation(libs.core.ktx)
    api(libs.material)
    implementation(libs.oss.licenses)
    implementation(libs.play.services.wearable)
    api(libs.timber)
    implementation(platform(libs.compose.bom))
    implementation(platform(libs.firebase.bom))

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit) { exclude(group = "org.hamcrest") }
    testImplementation(libs.mockito.kotlin)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)
}
