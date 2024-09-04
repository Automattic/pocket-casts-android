plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.settings"
    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }
}

dependencies {
    api(libs.appcompat)
    implementation(libs.compose.animation)
    implementation(libs.compose.icons)
    implementation(libs.compose.material)
    implementation(libs.compose.rxjava2)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.rx2)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.config)
    implementation(libs.fragment.ktx)
    implementation(libs.fragment.compose)
    api(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.reactivestreams.java)
    implementation(libs.okhttp)
    implementation(libs.rxandroid)
    implementation(libs.rxjava)
    implementation(libs.rxrelay)
    implementation(libs.rxkotlin)
    api(libs.cardview)
    implementation(libs.cast)
    api(libs.constraintlayout)
    implementation(libs.core.ktx)
    api(libs.material)
    implementation(libs.material.dialogs)
    implementation(libs.oss.licenses)
    implementation(libs.play.services.wearable)
    api(libs.preference)
    api(libs.recyclerview)
    implementation(libs.showkase)
    api(libs.crashlogging)
    implementation(libs.timber)
    implementation(platform(libs.compose.bom))
    implementation(platform(libs.firebase.bom))

    debugImplementation(libs.compose.ui.tooling)

    debugProdImplementation(libs.compose.ui.tooling)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit) { exclude(group = "org.hamcrest") }
    testImplementation(libs.mockito.kotlin)
    testImplementation("org.mockito:mockito-core:5.7.0")

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)
    ksp(libs.moshi.kotlin.codegen)
    ksp(libs.showkase.processor)

    api(project(":modules:services:analytics"))
    api(project(":modules:services:compose"))
    implementation(project(":modules:services:images"))
    implementation(project(":modules:services:localization"))
    api(project(":modules:services:model"))
    api(project(":modules:services:preferences"))
    api(project(":modules:services:servers"))
    api(project(":modules:services:repositories"))
    api(project(":modules:services:utils"))
    api(project(":modules:services:ui"))
    api(project(":modules:services:views"))
    testImplementation(project(":modules:services:sharedtest"))
}
