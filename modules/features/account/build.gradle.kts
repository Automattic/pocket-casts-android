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
    implementation(projects.modules.features.cartheme)
    api(projects.modules.features.settings)
    api(projects.modules.features.search)

    // services
    api(projects.modules.services.analytics)
    api(projects.modules.services.compose)
    implementation(projects.modules.services.images)
    implementation(projects.modules.services.localization)
    api(projects.modules.services.model)
    api(projects.modules.services.preferences)
    api(projects.modules.services.repositories)
    api(projects.modules.services.servers)
    api(projects.modules.services.ui)
    implementation(projects.modules.services.utils)
    api(projects.modules.services.views)
    testImplementation(projects.modules.services.sharedtest)

    // android libs
    api(libs.horologist.auth.data.phone)
    api(libs.horologist.datalayer)
}
