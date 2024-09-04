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
    implementation(libs.coroutines.core)
    api(libs.dagger.hilt.android)
    api(libs.moshi)
    implementation(libs.moshi.adapters)
    api(libs.okhttp)
    implementation(libs.okHttp.logging)
    api(libs.protobuf.javalite)
    api(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.retrofit.rx2)
    api(libs.rx2.java)
    implementation(libs.rx2.android)
    api(libs.work.runtime)
    implementation(libs.retrofit.protobuf) { exclude(group = "com.google.protobuf", module = "protobuf-java") }
    implementation(libs.timber)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)
    ksp(libs.moshi.kotlin.codegen)
    implementation(libs.crashlogging)


    implementation(project(":modules:services:localization"))
    api(project(":modules:services:model"))
    api(project(":modules:services:preferences"))
    api(project(":modules:services:protobuf"))
    implementation(project(":modules:services:utils"))
}
