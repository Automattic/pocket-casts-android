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
    implementation(libs.androidx.appcompat)
    implementation(libs.play.auth)
    api(libs.billing.ktx)
    api(libs.billing.ktx)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.rx2)
    api(libs.dagger.hilt.android)
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
    implementation(libs.protobuf.kotlinlite)
    implementation(libs.protobuf.javalite)
    implementation(libs.retrofit)
    implementation(libs.room.ktx)
    implementation(libs.rx2.android)
    api(libs.rx2.java)
    api(libs.rx2.relay)
    implementation(libs.rx2.kotlin)
    api(libs.work.runtime)
    api(libs.work.rx2)
    implementation(libs.play.cast)
    implementation(libs.lifecycle.reactivestreams.ktx)
    implementation(libs.coil)
    implementation(libs.compose.ui.graphics)
    implementation(libs.androidx.core.ktx)
    implementation(libs.device.names)
    implementation(libs.guava)
    implementation(libs.material)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.timber)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.turbine)
    testImplementation(libs.mockito.core)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    implementation(libs.room.ktx)
    api(projects.modules.services.analytics)
    api(projects.modules.services.crashlogging)
    implementation(projects.modules.services.deeplink)
    implementation(projects.modules.services.images)
    api(projects.modules.services.localization)
    api(projects.modules.services.model)
    api(projects.modules.services.preferences)
    api(projects.modules.services.protobuf)
    api(projects.modules.services.servers)
    api(projects.modules.services.utils)
    testImplementation(projects.modules.services.sharedtest)
}
