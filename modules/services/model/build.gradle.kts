plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.parcelize)
}

apply(from = "${project.rootDir}/base.gradle")

android {
    namespace = "au.com.shiftyjelly.pocketcasts.model"
    defaultConfig {
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":modules:services:localization"))
    implementation(project(":modules:services:utils"))

    api(libs.room.ktx)
    implementation(libs.room.ktx)
    implementation(libs.room.rxjava2)
    testImplementation(project(":modules:services:sharedtest"))

    ksp(libs.room.compiler)
}
