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
apply(plugin = "com.google.android.gms.oss-licenses-plugin")

android {
    namespace = "au.com.shiftyjelly.pocketcasts"

    defaultConfig {
        applicationId = project.property("applicationId").toString()
        multiDexEnabled = true
    }

    sourceSets {
        getByName("androidTest") {
            assets.srcDir(files("$rootDir/modules/services/model/schemas"))
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
        dataBinding = true
        compose = true
    }

    buildTypes {
        named("debug") {
            applicationIdSuffix = ".debug"

            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher_radioactive"
            manifestPlaceholders["sentryDsn"] = ""
        }

//        named("debugProd") {
//            initWith(getByName("debug"))
//            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher_3"
//        }

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
}

sentry {
    includeProguardMapping = file("$rootDir/sentry.properties").exists()
    tracingInstrumentation {
        features.set(EnumSet.allOf(InstrumentationFeature::class.java) - InstrumentationFeature.FILE_IO)
    }
}

dependencies {
    // features
    implementation(project(":modules:features:account"))
    implementation(project(":modules:features:discover"))
    implementation(project(":modules:features:endofyear"))
    implementation(project(":modules:features:filters"))
    implementation(project(":modules:features:navigation"))
    implementation(project(":modules:features:player"))
    implementation(project(":modules:features:podcasts"))
    implementation(project(":modules:features:profile"))
    implementation(project(":modules:features:search"))
    implementation(project(":modules:features:settings"))
    implementation(project(":modules:features:shared"))
    implementation(project(":modules:features:taskerplugin"))
    // services
    implementation(project(":modules:services:analytics"))
    implementation(project(":modules:services:compose"))
    implementation(project(":modules:services:localization"))
    implementation(project(":modules:services:model"))
    implementation(project(":modules:services:preferences"))
    implementation(project(":modules:services:repositories"))
    implementation(project(":modules:services:servers"))
    implementation(project(":modules:services:utils"))
    implementation(project(":modules:services:ui"))
    implementation(project(":modules:services:views"))
    testImplementation(project(":modules:services:sharedtest"))
}
