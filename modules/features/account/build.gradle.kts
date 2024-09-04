plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.account"
    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }
}

dependencies {
    api(libs.androidx.appcompat)
    implementation(libs.play.auth)
    implementation(libs.compose.activity)
    implementation(libs.compose.animation)
    implementation(libs.compose.constraintlayout)
    implementation(libs.compose.livedata)
    api(libs.compose.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.ui.util)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.play.services)
    implementation(libs.coroutines.rx2)
    api(libs.navigation.runtime)
    implementation(libs.fragment.ktx)
    api(libs.dagger.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    api(libs.moshi)
    api(libs.navigation.runtime)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    implementation(libs.navigation.compose)
    implementation(libs.retrofit)
    implementation(libs.rx2.android)
    implementation(libs.rx2.java)
    implementation(libs.rx2.relay)
    implementation(libs.rx2.kotlin)
    implementation(libs.play.cast)
    implementation(libs.coil)
    api(libs.androidx.constraintlayout)
    implementation(libs.lifecycle.reactivestreams.ktx)
    implementation(libs.androidx.core.ktx)
    api(libs.material)
    implementation(libs.androidx.preference.ktx)
    api(libs.showkase)
    implementation(libs.timber)
    implementation(platform(libs.compose.bom))

    debugImplementation(libs.compose.ui.tooling)

    debugProdImplementation(libs.compose.ui.tooling)

    testImplementation(libs.mockito.core)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.turbine)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)
    ksp(libs.moshi.kotlin.codegen)
    ksp(libs.showkase.processor)

    // features
    implementation(project(":modules:features:cartheme"))
    api(project(":modules:features:settings"))
    api(project(":modules:features:search"))

    // services
    api(project(":modules:services:analytics"))
    api(project(":modules:services:compose"))
    implementation(project(":modules:services:images"))
    implementation(project(":modules:services:localization"))
    api(project(":modules:services:model"))
    api(project(":modules:services:preferences"))
    api(project(":modules:services:repositories"))
    api(project(":modules:services:servers"))
    api(project(":modules:services:ui"))
    implementation(project(":modules:services:utils"))
    api(project(":modules:services:views"))
    testImplementation(project(":modules:services:sharedtest"))

    // android libs
    api(libs.horologist.auth.data.phone)
    api(libs.horologist.datalayer)
}
