import io.sentry.android.gradle.extensions.InstrumentationFeature
import java.util.EnumSet

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kapt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.sentry)
    alias(libs.plugins.google.services)
    alias(libs.plugins.kotlin.parcelize)
}

apply(from = "../base.gradle")

android {
    namespace = "au.com.shiftyjelly.pocketcasts"

    defaultConfig {
        minSdk = project.property("minSdkVersionWear") as Int
        applicationId = project.property("applicationId").toString()
    }

    buildTypes {
        named("debug") {
            applicationIdSuffix = ".debug"

            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher_radioactive"
            manifestPlaceholders["sentryDsn"] = ""
        }

        named("debugProd") {
            initWith(getByName("debug"))
        }

        named("release") {
            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher"
            val pocketcastsSentryDsn: String by project
            if (pocketcastsSentryDsn.isNotBlank()) {
                manifestPlaceholders["sentryDsn"] = pocketcastsSentryDsn
            }
            else {
                println("WARNING: Sentry DSN gradle property 'pocketcastsSentryDsn' not found. Crash reporting won't work without this.")
            }

            if (!file("${project.rootDir}/sentry.properties").exists()) {
                println("WARNING: Sentry configuration file 'sentry.properties' not found. The ProGuard mapping files won't be uploaded.")
            }

            proguardFiles.addAll(
                listOf(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    file("proguard-rules.pro")
                )
            )
            isShrinkResources = true
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
        dataBinding = true
        compose=  true
    }

    kotlinOptions {
        jvmTarget = "1.8"
        // Allow for widescale experimental APIs in Alpha libraries we build upon
        freeCompilerArgs += "-opt-in=com.google.android.horologist.annotations.ExperimentalHorologistApi"
        freeCompilerArgs += "-opt-in=com.google.android.horologist.media.ui.ExperimentalHorologistMediaUiApi"
    }
}

sentry {
    includeProguardMapping = file("$rootDir/sentry.properties").exists()
    tracingInstrumentation {
        features.set(EnumSet.allOf(InstrumentationFeature::class.java) - InstrumentationFeature.FILE_IO)
    }
}

dependencies {
    implementation(libs.wear.input)
    implementation(libs.wear.remote.interactions)

    // General Compose dependencies
    implementation(libs.compose.activity)
    implementation(libs.compose.ui)
    implementation(libs.compose.icons)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.hilt.navigation.compose)

    // Compose for Wear OS Dependencies
    implementation(libs.bundles.wear.compose)

    implementation(libs.bundles.horologist)
    implementation(libs.media3.datasource.okhttp)
    implementation(project(":modules:services:analytics"))
    implementation(project(":modules:services:compose"))
    implementation(project(":modules:services:images"))
    implementation(project(":modules:services:localization"))
    implementation(project(":modules:services:model"))
    implementation(project(":modules:services:preferences"))
    implementation(project(":modules:services:repositories"))
    implementation(project(":modules:services:servers"))
    implementation(project(":modules:services:ui"))
    implementation(project(":modules:services:utils"))
    testImplementation(project(":modules:services:sharedtest"))

    implementation(project(":modules:features:account"))
    implementation(project(":modules:features:podcasts"))
    implementation(project(":modules:features:profile"))
    implementation(project(":modules:features:settings"))
    implementation(project(":modules:features:shared"))
}
