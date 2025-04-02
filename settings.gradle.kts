import java.net.URI
import java.util.Properties

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven {
            setUrl("https://a8c-libs.s3.amazonaws.com/android")
            content {
                includeGroup("com.automattic.android")
                includeGroup("com.automattic.android.measure-builds")
            }
        }
    }
}

plugins {
    id("com.gradle.develocity").version("3.19")
}

apply(from = File("./config/gradle/gradle_build_scan.gradle"))

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            setUrl("https://a8c-libs.s3.amazonaws.com/android")
            content {
                includeGroup("com.automattic")
                includeGroup("com.automattic.tracks")
                includeGroupByRegex("org.wordpress.*")
            }
        }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "pocket-casts-android"

include(":app")
include(":automotive")
include(":wear")

// features
include(":modules:features:account")
include(":modules:features:cartheme")
include(":modules:features:discover")
include(":modules:features:endofyear")
include(":modules:features:engage")
include(":modules:features:filters")
include(":modules:features:navigation")
include(":modules:features:nova")
include(":modules:features:player")
include(":modules:features:podcasts")
include(":modules:features:profile")
include(":modules:features:search")
include(":modules:features:settings")
include(":modules:features:shared")
include(":modules:features:reimagine")
include(":modules:features:referrals")
include(":modules:features:taskerplugin")
include(":modules:features:widgets")

// services
include(":modules:services:analytics")
include(":modules:services:compose")
include(":modules:services:crashlogging")
include(":modules:services:deeplink")
include(":modules:services:images")
include(":modules:services:localization")
include(":modules:services:media-noop")
include(":modules:services:model")
include(":modules:services:preferences")
include(":modules:services:protobuf")
include(":modules:services:repositories")
include(":modules:services:sharing")
include(":modules:services:servers")
include(":modules:services:ui")
include(":modules:services:utils")
include(":modules:services:views")
include(":modules:services:sharedtest")

val developerProperties = loadPropertiesFromFile(File("${rootDir.path}/developer.properties"))
val secretProperties = loadPropertiesFromFile(File("${rootDir.path}/secret.properties"))
val USE_REMOTE_BUILD_CACHE_LOCALLY = "use_remote_build_cache_locally"

buildCache {
    if (System.getenv("CI")?.toBoolean() == true) {
        remote<HttpBuildCache> {
            url = URI.create("http://10.0.2.214:5071/cache/")
            isAllowUntrustedServer = true
            isAllowInsecureProtocol = true
            isPush = true
            credentials {
                username = "ci-user"
                password = System.getenv("GRADLE_CACHE_NODE_PASSWORD")
            }
        }
    } else if (developerProperties.getProperty(USE_REMOTE_BUILD_CACHE_LOCALLY).toBoolean()) {
        remote<HttpBuildCache> {
            url = URI.create(secretProperties.getProperty("gradleCacheNodeUrl").ifEmpty { throw IllegalArgumentException("Gradle Cache Node URL is missing. Make sure to apply secrets `bundle exec fastlane run configure_apply`.") })
            isPush = false
            credentials {
                username = "developer"
                password = secretProperties.getProperty("gradleCacheNodePassword")
            }
        }
    }
}

private fun loadPropertiesFromFile(file: File): Properties {
    val properties = Properties()
    if (file.exists()) {
        file.inputStream().use { stream ->
            properties.load(stream)
        }
    }
    return properties
}
