plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.discover"
    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = false
    }
}

dependencies {
    api(libs.androidx.appcompat)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.rx2)
    implementation(libs.fragment.ktx)
    api(libs.dagger.hilt.android)
    api(libs.hilt.work)
    testImplementation(libs.moshi)
    testImplementation(libs.okhttp)
    testImplementation(libs.retrofit)
    testImplementation(libs.retrofit.moshi)
    implementation(libs.rx2.android)
    api(libs.rx2.java)
    implementation(libs.rx2.relay)
    implementation(libs.rx2.kotlin)
    api(libs.work.runtime)
    api(libs.androidx.cardview)
    implementation(libs.coil)
    api(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    api(libs.material)
    implementation(libs.material.dialogs)
    implementation(libs.material.progressbar)
    implementation(libs.androidx.preference.ktx)
    api(libs.androidx.recyclerview)
    implementation(libs.timber)
    api(libs.crashlogging)
    implementation(platform(libs.compose.bom))

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.okHttp.mockwebserver)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    // features
    implementation(project(":modules:features:podcasts"))
    implementation(project(":modules:features:search"))

    // services
    api(project(":modules:services:analytics"))
    implementation(project(":modules:services:localization"))
    implementation(project(":modules:services:images"))
    api(project(":modules:services:model"))
    api(project(":modules:services:preferences"))
    api(project(":modules:services:repositories"))
    api(project(":modules:services:servers"))
    implementation(project(":modules:services:utils"))
    api(project(":modules:services:ui"))
    api(project(":modules:services:views"))
}
