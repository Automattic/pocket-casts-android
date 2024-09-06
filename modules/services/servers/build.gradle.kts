plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.servers"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)
    ksp(libs.moshi.kotlin.codegen)

    api(libs.dagger.hilt.android)
    api(libs.moshi)
    api(libs.okhttp)
    api(libs.protobuf.javalite)
    api(libs.retrofit)
    api(libs.rx2.java)
    api(libs.work.runtime)

    api(projects.modules.services.model)
    api(projects.modules.services.preferences)
    api(projects.modules.services.protobuf)

    implementation(libs.automattic.crashlogging)
    implementation(libs.coroutines.core)
    implementation(libs.moshi.adapters)
    implementation(libs.okHttp.logging)
    implementation(libs.retrofit.moshi)
    implementation(libs.retrofit.protobuf) { exclude(group = "com.google.protobuf", module = "protobuf-java") }
    implementation(libs.retrofit.rx2)
    implementation(libs.rx2.android)
    implementation(libs.timber)

    implementation(projects.modules.services.localization)
    implementation(projects.modules.services.utils)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
}
