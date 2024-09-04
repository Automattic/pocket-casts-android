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
    implementation(libs.dagger.hilt.core)

    implementation(libs.play.auth)
    implementation(libs.coroutines.core)
    api(libs.billing.ktx)
    api(libs.media3.extractor)
    api(libs.moshi)
    api(libs.okhttp)
    api(libs.room)
    implementation(libs.room.rx2)
    api(libs.rx2.java)
    implementation(libs.play.cast)
    implementation(libs.androidx.core.ktx)
    implementation(libs.timber)
    compileOnly(libs.media3.common)

    debugImplementation(libs.compose.ui.tooling)

    debugProdImplementation(libs.compose.ui.tooling)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)
    ksp(libs.moshi.kotlin.codegen)

    androidTestImplementation(libs.androidx.annotation)
    androidTestImplementation(platform(libs.compose.bom))

    implementation(project(":modules:services:localization"))
    api(project(":modules:services:utils"))

    implementation(libs.room.ktx)
    implementation(libs.room.rx2)
    testImplementation(project(":modules:services:sharedtest"))

    testImplementation(libs.mockito.core)

    ksp(libs.room.compiler)
}
