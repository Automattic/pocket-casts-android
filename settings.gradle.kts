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
    id("com.gradle.develocity").version("4.2.1")
    id("com.gradle.common-custom-user-data-gradle-plugin").version("2.4.0")
}

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
include(":modules:features:ads")
include(":modules:features:appreview")
include(":modules:features:cartheme")
include(":modules:features:discover")
include(":modules:features:endofyear")
include(":modules:features:engage")
include(":modules:features:filters")
include(":modules:features:navigation")
include(":modules:features:player")
include(":modules:features:podcasts")
include(":modules:features:profile")
include(":modules:features:search")
include(":modules:features:settings")
include(":modules:features:shared")
include(":modules:features:reimagine")
include(":modules:features:referrals")
include(":modules:features:taskerplugin")
include(":modules:features:transcripts")
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
include(":modules:services:payment")
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
val secretsFile = File("${rootDir.path}/secret.properties")
val secretProperties = loadPropertiesFromFile(secretsFile)
val useRemoteBuildCacheLocally = "use_remote_build_cache_locally"

gradle.extra["isCi"] = System.getenv("CI")?.toBoolean() ?: false
gradle.extra["develocityToken"] = secretProperties.getProperty("develocityToken")
gradle.extra["measureBuildsEnabled"] = secretProperties.getProperty("measureBuildsEnabled")

apply(from = File("./config/gradle/gradle_build_scan.gradle"))

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
    } else if (developerProperties.getProperty(useRemoteBuildCacheLocally).toBoolean()) {

        checkForRemoteBuildCacheOptimizedExperience()

        remote<HttpBuildCache> {
            url = URI.create(secretProperties.getProperty("gradleCacheNodeUrl"))
            isPush = false
            credentials {
                username = "developer"
                password = secretProperties.getProperty("gradleCacheNodePassword")
            }
        }
    } else {
        logger.warn("\nℹ️ Remote build cache is disabled. If you have stable internet connection, consider enabling it via `developer.properties`.")
    }
}

private fun checkForRemoteBuildCacheOptimizedExperience() {
    assertSecretsApplied()
    assertJava21Amazon()
}

private fun assertSecretsApplied() {
    if (!secretsFile.exists()) {
        throw GradleException("The build requested remote build cache, but secrets file is not found. Please run `bundle exec fastlane run configure_apply` to apply secrets.")
    }
}

private fun assertJava21Amazon() {
    val version = System.getProperty("java.version")
    val vendor = System.getProperty("java.vendor")
    val expectedJdkVersion = "21.0.6"

    if (!(version.contains(expectedJdkVersion) && vendor.contains("amazon", ignoreCase = true))) {
        logger.error("Java version: $version, vendor: $vendor")
        throw GradleException("Java version is not $expectedJdkVersion or vendor is not Amazon Corretto. This significantly reduces efficiency of remote build cache. Please set up the matching JDK.")
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
