plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose.compiler)
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
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)
    ksp(libs.moshi.kotlin.codegen)
    ksp(libs.showkase.processor)

    api(libs.androidx.appcompat)
    api(libs.androidx.constraintlayout)
    api(libs.compose.material)
    api(libs.dagger.hilt.android)
    api(libs.horologist.auth.data.phone)
    api(libs.horologist.datalayer)
    api(libs.material)
    api(libs.moshi)
    api(libs.navigation.runtime)
    api(libs.showkase)

    api(projects.modules.features.search)
    api(projects.modules.features.settings)
    api(projects.modules.services.analytics)
    api(projects.modules.services.compose)
    api(projects.modules.services.model)
    api(projects.modules.services.preferences)
    api(projects.modules.services.repositories)
    api(projects.modules.services.servers)
    api(projects.modules.services.ui)
    api(projects.modules.services.views)

    implementation(platform(libs.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.coil)
    implementation(libs.compose.activity)
    implementation(libs.compose.animation)
    implementation(libs.compose.constraintlayout)
    implementation(libs.compose.livedata)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.ui.util)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.play.services)
    implementation(libs.coroutines.rx2)
    implementation(libs.fragment.ktx)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.lifecycle.reactivestreams.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    implementation(libs.play.auth)
    implementation(libs.play.cast)
    implementation(libs.retrofit)
    implementation(libs.rx2.android)
    implementation(libs.rx2.java)
    implementation(libs.rx2.kotlin)
    implementation(libs.rx2.relay)
    implementation(libs.timber)

    implementation(projects.modules.features.cartheme)
    implementation(projects.modules.services.images)
    implementation(projects.modules.services.localization)
    implementation(projects.modules.services.utils)

    debugImplementation(libs.compose.ui.tooling)

    debugProdImplementation(libs.compose.ui.tooling)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.turbine)

    testImplementation(projects.modules.services.sharedtest)
}
