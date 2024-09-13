plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.sentry)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts"

    defaultConfig {
        minSdk = project.property("minSdkVersionAutomotive") as Int
        applicationId = project.property("applicationId").toString()
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }

    buildTypes {
        named("debug") {
            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher_radioactive"
        }

        named("debugProd") {
            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher_radioactive"
        }

        named("release") {
            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher"
        }
    }
}

dependencies {
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    implementation(platform(libs.compose.bom))
    implementation(platform(libs.firebase.bom))

    implementation(libs.aboutlibraries.compose)
    implementation(libs.aboutlibraries.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.viewpager)
    implementation(libs.automattic.crashlogging)
    implementation(libs.compose.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.rx2)
    implementation(libs.dagger.hilt.android)
    implementation(libs.dagger.hilt.core)
    implementation(libs.encryptedlogging)
    implementation(libs.guava)
    implementation(libs.hilt.work)
    implementation(libs.lifecycle.reactivestreams.ktx)
    implementation(libs.material)
    implementation(libs.material.progressbar)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.extractor)
    implementation(libs.media3.ui)
    implementation(libs.moshi)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.rx2.java)
    implementation(libs.timber)
    implementation(libs.work.runtime)

    implementation(projects.modules.features.account)
    implementation(projects.modules.features.discover)
    implementation(projects.modules.features.player)
    implementation(projects.modules.features.podcasts)
    implementation(projects.modules.features.profile)
    implementation(projects.modules.features.search)
    implementation(projects.modules.features.settings)
    implementation(projects.modules.features.shared)
    implementation(projects.modules.services.analytics)
    implementation(projects.modules.services.compose)
    implementation(projects.modules.services.crashlogging)
    implementation(projects.modules.services.localization)
    implementation(projects.modules.services.mediaNoop)
    implementation(projects.modules.services.model)
    implementation(projects.modules.services.preferences)
    implementation(projects.modules.services.repositories)
    implementation(projects.modules.services.servers)
    implementation(projects.modules.services.sharing)
    implementation(projects.modules.services.ui)
    implementation(projects.modules.services.utils)
    implementation(projects.modules.services.views)

    debugImplementation(libs.compose.ui.tooling)

    debugProdImplementation(libs.compose.ui.tooling)

    androidTestImplementation(libs.androidx.annotation)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.junit.ext)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.mockito.core)
    androidTestImplementation(libs.mockito.kotlin)
}

apply(plugin = "com.google.gms.google-services")
