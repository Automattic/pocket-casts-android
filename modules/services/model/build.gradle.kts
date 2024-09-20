plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.model"
    defaultConfig {
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)
    ksp(libs.moshi.kotlin.codegen)
    ksp(libs.room.compiler)

    api(libs.billing.ktx)
    api(libs.media3.extractor)
    api(libs.moshi)
    api(libs.okhttp)
    api(libs.room)
    api(libs.rx2.java)

    api(projects.modules.services.utils)

    implementation(libs.androidx.core.ktx)
    implementation(libs.coroutines.core)
    implementation(libs.dagger.hilt.core)
    implementation(libs.play.auth)
    implementation(libs.play.cast)
    implementation(libs.room.ktx)
    implementation(libs.room.rx2)
    implementation(libs.timber)

    implementation(projects.modules.services.localization)

    debugImplementation(libs.compose.ui.tooling)

    debugProdImplementation(libs.compose.ui.tooling)

    compileOnly(libs.media3.common)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)

    testImplementation(projects.modules.services.sharedtest)

    androidTestImplementation(libs.androidx.annotation)
}
