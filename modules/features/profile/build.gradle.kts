plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.parcelize)
}

apply(from = "${project.rootDir}/base.gradle")

android {
    namespace = "au.com.shiftyjelly.pocketcasts.profile"
    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }
}

dependencies {
    // features
    implementation(project(":modules:features:account"))
    implementation(project(":modules:features:cartheme"))
    implementation(project(":modules:features:endofyear"))
    implementation(project(":modules:features:player"))
    implementation(project(":modules:features:podcasts"))
    implementation(project(":modules:features:settings"))
    implementation(libs.compose.material3)

    // services
    implementation(project(":modules:services:analytics"))
    implementation(project(":modules:services:compose"))
    implementation(project(":modules:services:crashlogging"))
    implementation(project(":modules:services:images"))
    implementation(project(":modules:services:localization"))
    implementation(project(":modules:services:model"))
    implementation(project(":modules:services:preferences"))
    implementation(project(":modules:services:repositories"))
    implementation(project(":modules:services:servers"))
    implementation(project(":modules:services:ui"))
    implementation(project(":modules:services:utils"))
    implementation(project(":modules:services:views"))

    implementation("com.gravatar:gravatar-ui:trunk-9fefe1ad77827a346b29916767bbe7a0f100bf17")
    implementation("com.gravatar:gravatar:trunk-9fefe1ad77827a346b29916767bbe7a0f100bf17")
}
