plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.repositories"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.auth)
    api(libs.billing)
    api(libs.billing.ktx)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.rx2)
    api(libs.hilt.android)
    api(libs.hilt.work)
    implementation(libs.lifecycle.process)
    api(libs.media3.datasource)
    implementation(libs.media3.datasource.okhttp)
    api(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    api(libs.media3.extractor)
    implementation(libs.media3.ui)
    api(libs.moshi)
    api(libs.okhttp)
    implementation(libs.protobuf.kotlin.lite)
    implementation(libs.protobuf.javalite)
    implementation(libs.retrofit)
    implementation(libs.room.ktx)
    implementation(libs.rxandroid)
    api(libs.rxjava)
    api(libs.rxrelay)
    implementation(libs.rxkotlin)
    api(libs.work.runtime)
    api(libs.work.rxjava2)
    implementation(libs.cast)
    implementation(libs.lifecycle.reactivestreams.java)
    implementation(libs.coil)
    implementation("androidx.compose.ui:ui-graphics:1.6.2")
    implementation(libs.core.ktx)
    implementation(libs.device.names)
    implementation(libs.guava)
    implementation(libs.material)
    implementation(libs.oss.licenses)
    implementation(libs.play.services.wearable)
    implementation(libs.preference)
    implementation(libs.timber)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.turbine)
    testImplementation("org.mockito:mockito-core:5.7.0")

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    implementation(libs.room.ktx)
    api(project(":modules:services:analytics"))
    api(project(":modules:services:crashlogging"))
    implementation(project(":modules:services:deeplink"))
    implementation(project(":modules:services:images"))
    api(project(":modules:services:localization"))
    api(project(":modules:services:model"))
    api(project(":modules:services:preferences"))
    api(project(":modules:services:protobuf"))
    api(project(":modules:services:servers"))
    api(project(":modules:services:utils"))
    testImplementation(project(":modules:services:sharedtest"))
}
