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
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    api(libs.billing.ktx)
    api(libs.dagger.hilt.android)
    api(libs.hilt.work)
    api(libs.media3.datasource)
    api(libs.media3.exoplayer)
    api(libs.media3.extractor)
    api(libs.moshi)
    api(libs.okhttp)
    api(libs.rx2.java)
    api(libs.rx2.relay)
    api(libs.work.runtime)
    api(libs.work.rx2)

    api(projects.modules.services.analytics)
    api(projects.modules.services.crashlogging)
    api(projects.modules.services.localization)
    api(projects.modules.services.model)
    api(projects.modules.services.preferences)
    api(projects.modules.services.protobuf)
    api(projects.modules.services.servers)
    api(projects.modules.services.utils)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.coil)
    implementation(libs.compose.ui.graphics)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.rx2)
    implementation(libs.device.names)
    implementation(libs.guava)
    implementation(libs.lifecycle.process)
    implementation(libs.lifecycle.reactivestreams.ktx)
    implementation(libs.material)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.ui)
    implementation(libs.play.auth)
    implementation(libs.play.cast)
    implementation(libs.protobuf.javalite)
    implementation(libs.protobuf.kotlinlite)
    implementation(libs.retrofit)
    implementation(libs.room.ktx)
    implementation(libs.rx2.android)
    implementation(libs.rx2.kotlin)
    implementation(libs.timber)

    implementation(projects.modules.services.deeplink)
    implementation(projects.modules.services.images)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.turbine)

    testImplementation(projects.modules.services.sharedtest)
}
