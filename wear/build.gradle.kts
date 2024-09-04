plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.sentry)
    alias(libs.plugins.google.services)
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.coroutines.reactive)
    implementation(libs.coroutines.android)
    implementation(libs.crashlogging)
    implementation(libs.encryptedlogging)
    implementation(libs.dagger.hilt.core)
    implementation(libs.horologist.auth.data.phone)
    implementation(libs.navigation.runtime)
    implementation(libs.rx2.java)
    implementation(libs.work.runtime)

    implementation(libs.play.auth)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.rx2)
    implementation(libs.firebase.config.ktx)
    implementation(libs.dagger.hilt.android)
    implementation(libs.lottie.compose)
    implementation(libs.coil.compose)
    implementation(libs.androidx.palette)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.guava)
    implementation(libs.hilt.work)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lottie)
    implementation(libs.media3.extractor)
    implementation(libs.moshi)
    implementation(libs.navigation.compose)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.rx2.relay)
    implementation(libs.timber)
    implementation(platform(libs.compose.bom))
    implementation(platform(libs.firebase.bom))

    debugImplementation(libs.compose.ui.tooling)

    debugProdImplementation(libs.compose.ui.tooling)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.turbine)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    implementation(libs.wear.input)
    implementation(libs.wear.remote.interactions)
    implementation(libs.wear.tooling.preview)

    // General Compose dependencies
    implementation(libs.compose.animation)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.livedata)
    implementation(libs.compose.material)
    implementation(libs.compose.rxjava2)
    implementation(libs.compose.activity)
    implementation(libs.compose.ui)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.hilt.navigation.compose)

    // Compose for Wear OS Dependencies
    implementation(libs.wear.compose.material)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.navigation)

    testImplementation(libs.mockito.core)


    implementation(libs.horologist.audio.ui)
    implementation(libs.horologist.auth.composables)
    implementation(libs.horologist.auth.data)
    implementation(libs.horologist.auth.ui)
    implementation(libs.horologist.compose.layout)
    implementation(libs.horologist.compose.material)
    implementation(libs.horologist.datalayer)
    implementation(libs.horologist.media)
    implementation(libs.horologist.audio)
    implementation(libs.horologist.media3.outputswitcher)
    implementation(libs.horologist.media.ui)
    implementation(libs.horologist.network.awarness.okhttp)

    implementation(project(":modules:services:analytics"))
    implementation(project(":modules:services:compose"))
    implementation(project(":modules:services:crashlogging"))
    implementation(project(":modules:services:images"))
    implementation(project(":modules:services:localization"))
    implementation(project(":modules:services:media-noop"))
    implementation(project(":modules:services:model"))
    implementation(project(":modules:services:preferences"))
    implementation(project(":modules:services:repositories"))
    implementation(project(":modules:services:servers"))
    implementation(project(":modules:services:ui"))
    implementation(project(":modules:services:utils"))
    implementation(project(":modules:services:views"))
    testImplementation(project(":modules:services:sharedtest"))

    implementation(project(":modules:features:account"))
    implementation(project(":modules:features:podcasts"))
    implementation(project(":modules:features:profile"))
    implementation(project(":modules:features:settings"))
    implementation(project(":modules:features:shared"))
    implementation(project(":modules:features:player"))
    implementation(project(":modules:features:search"))
    implementation(project(":modules:services:sharing"))
}
