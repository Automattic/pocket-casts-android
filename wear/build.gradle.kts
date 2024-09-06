plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.sentry)
    alias(libs.plugins.google.services)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts"

    defaultConfig {
        minSdk = project.property("minSdkVersionWear") as Int
        applicationId = project.property("applicationId").toString()
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

            if (!file("${project.rootDir}/sentry.properties").exists()) {
                println("WARNING: Sentry configuration file 'sentry.properties' not found. The ProGuard mapping files won't be uploaded.")
            }
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }

    kotlinOptions {
        // Allow for widescale experimental APIs in Alpha libraries we build upon
        freeCompilerArgs += "-opt-in=com.google.android.horologist.annotations.ExperimentalHorologistApi"
    }
}

dependencies {
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    implementation(platform(libs.compose.bom))
    implementation(platform(libs.firebase.bom))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.palette)
    implementation(libs.automattic.crashlogging)
    implementation(libs.coil.compose)
    implementation(libs.compose.activity)
    implementation(libs.compose.animation)
    implementation(libs.compose.livedata)
    implementation(libs.compose.material)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.rxjava2)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.reactive)
    implementation(libs.coroutines.rx2)
    implementation(libs.dagger.hilt.android)
    implementation(libs.dagger.hilt.core)
    implementation(libs.encryptedlogging)
    implementation(libs.firebase.config.ktx)
    implementation(libs.guava)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    implementation(libs.horologist.audio)
    implementation(libs.horologist.audio.ui)
    implementation(libs.horologist.auth.composables)
    implementation(libs.horologist.auth.data)
    implementation(libs.horologist.auth.data.phone)
    implementation(libs.horologist.auth.ui)
    implementation(libs.horologist.compose.layout)
    implementation(libs.horologist.compose.material)
    implementation(libs.horologist.datalayer)
    implementation(libs.horologist.media)
    implementation(libs.horologist.media.ui)
    implementation(libs.horologist.media3.outputswitcher)
    implementation(libs.horologist.network.awarness.okhttp)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lottie)
    implementation(libs.lottie.compose)
    implementation(libs.media3.extractor)
    implementation(libs.moshi)
    implementation(libs.navigation.compose)
    implementation(libs.navigation.runtime)
    implementation(libs.okhttp)
    implementation(libs.play.auth)
    implementation(libs.retrofit)
    implementation(libs.rx2.java)
    implementation(libs.rx2.relay)
    implementation(libs.timber)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.material)
    implementation(libs.wear.compose.navigation)
    implementation(libs.wear.input)
    implementation(libs.wear.remote.interactions)
    implementation(libs.wear.tooling.preview)
    implementation(libs.work.runtime)

    implementation(projects.modules.features.account)
    implementation(projects.modules.features.player)
    implementation(projects.modules.features.podcasts)
    implementation(projects.modules.features.profile)
    implementation(projects.modules.features.search)
    implementation(projects.modules.features.settings)
    implementation(projects.modules.features.shared)
    implementation(projects.modules.services.analytics)
    implementation(projects.modules.services.compose)
    implementation(projects.modules.services.crashlogging)
    implementation(projects.modules.services.images)
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

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.turbine)

    testImplementation(projects.modules.services.sharedtest)
}
