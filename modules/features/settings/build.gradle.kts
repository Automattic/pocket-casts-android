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
    api(libs.androidx.appcompat)
    implementation(libs.compose.animation)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.material)
    implementation(libs.compose.rxjava2)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.rx2)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.config.ktx)
    implementation(libs.fragment.ktx)
    implementation(libs.fragment.compose)
    api(libs.dagger.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.reactivestreams.ktx)
    implementation(libs.okhttp)
    implementation(libs.rx2.android)
    implementation(libs.rx2.java)
    implementation(libs.rx2.relay)
    implementation(libs.rx2.kotlin)
    api(libs.androidx.cardview)
    implementation(libs.play.cast)
    api(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    api(libs.material)
    implementation(libs.material.dialogs)
    implementation(libs.play.oss.licenses)
    api(libs.androidx.preference.ktx)
    api(libs.androidx.recyclerview)
    implementation(libs.showkase)
    api(libs.crashlogging)
    implementation(libs.timber)
    implementation(platform(libs.compose.bom))
    implementation(platform(libs.firebase.bom))

    debugImplementation(libs.compose.ui.tooling)

    debugProdImplementation(libs.compose.ui.tooling)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockito.core)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)
    ksp(libs.moshi.kotlin.codegen)
    ksp(libs.showkase.processor)

    api(projects.modules.services.analytics)
    api(projects.modules.services.compose)
    implementation(projects.modules.services.images)
    implementation(projects.modules.services.localization)
    api(projects.modules.services.model)
    api(projects.modules.services.preferences)
    api(projects.modules.services.servers)
    api(projects.modules.services.repositories)
    api(projects.modules.services.utils)
    api(projects.modules.services.ui)
    api(projects.modules.services.views)
    testImplementation(projects.modules.services.sharedtest)
}
