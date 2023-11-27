plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kapt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.sentry)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.kotlin.parcelize)
}

apply(from = "../base.gradle")

android {
    namespace = "au.com.shiftyjelly.pocketcasts"

    defaultConfig {
        applicationId = project.property("applicationId").toString()
        minSdk = 28
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

        named("debugProd") {
            initWith(getByName("debug"))
            isDebuggable = true
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

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // Commented out the Automotive library as it clashes with the Material library and we don't use it. Duplicate value for resource attr/navigationIconTint.
    //implementation "androidx.car:car:1.0.0-alpha7"
    implementation(libs.appcompat)
    implementation(libs.core.ktx)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.bundles.aboutlibraries)
    testImplementation(libs.junit)
    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.espresso.core)

    implementation(project(":modules:services:localization"))
    implementation(project(":modules:services:preferences"))
    implementation(project(":modules:services:utils"))
    implementation(project(":modules:services:model"))
    implementation(project(":modules:services:ui"))
    implementation(project(":modules:services:views"))
    implementation(project(":modules:services:compose"))
    implementation(project(":modules:services:repositories"))
    implementation(project(":modules:services:servers"))
    implementation(project(":modules:services:analytics"))
    implementation(project(":modules:features:profile"))
    implementation(project(":modules:features:podcasts"))
    implementation(project(":modules:features:settings"))
    implementation(project(":modules:features:discover"))
    implementation(project(":modules:features:account"))
    implementation(project(":modules:features:shared"))
}

apply(plugin = "com.google.gms.google-services")
