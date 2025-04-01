plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

gradlePlugin {
    plugins {
        register("remoteBuildCachePlugin") {
            id = "remote-build-cache-plugin"
            implementationClass = "com.pocketcasts.PocketCastsBuildCachePlugin"
        }
    }
}

dependencies {
    implementation(libs.gradle.develocity)
}