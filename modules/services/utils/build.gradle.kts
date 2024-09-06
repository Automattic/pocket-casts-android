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
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    api(libs.androidx.appcompat)
    api(libs.coil.base)
    api(libs.dagger.hilt.android)
    api(libs.material)
    api(libs.okhttp)
    api(libs.rx2.java)
    api(libs.timber)

    implementation(platform(libs.compose.bom))
    implementation(platform(libs.firebase.bom))

    implementation(libs.androidx.annotation)
    implementation(libs.androidx.car)
    implementation(libs.androidx.core.ktx)
    implementation(libs.compose.ui.graphics)
    implementation(libs.coroutines.core)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.config.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.play.auth)
    implementation(libs.play.cast)
    implementation(libs.rx2.android)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
}
