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
    implementation(libs.lifecycle.livedata.ktx)
    testImplementation(libs.mockito.core)

    api(libs.androidx.appcompat)
    implementation(libs.play.auth)
    implementation(libs.coroutines.core)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.config.ktx)
    api(libs.dagger.hilt.android)
    api(libs.okhttp)
    implementation(libs.rx2.android)
    api(libs.rx2.java)
    implementation(libs.androidx.car)
    implementation(libs.androidx.annotation)
    implementation(libs.play.cast)
    implementation(libs.compose.ui.graphics)
    implementation(libs.androidx.core.ktx)
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
