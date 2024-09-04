plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.preferences"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.coroutines.core)
    api(libs.dagger.hilt.android)
    api(libs.moshi)
    api(libs.rx2.java)
    api(libs.rx2.relay)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.preference.ktx)
    api(libs.work.runtime)
    implementation(libs.timber)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config.ktx)
    implementation(libs.play.cast)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    implementation(projects.modules.services.images)
    implementation(projects.modules.services.localization)
    api(projects.modules.services.model)
    api(projects.modules.services.utils)
}
