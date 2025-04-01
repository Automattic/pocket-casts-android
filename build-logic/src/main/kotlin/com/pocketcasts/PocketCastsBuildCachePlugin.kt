package com.pocketcasts

import java.io.File
import java.net.URI
import java.util.Properties
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.caching.http.HttpBuildCache
import org.gradle.kotlin.dsl.remote

class PocketCastsBuildCachePlugin : Plugin<Settings> {

    override fun apply(target: Settings) {

        val developerProperties = loadPropertiesFromFile(File("${target.rootDir.path}/developer.properties"))
        val secretProperties = loadPropertiesFromFile(File("${target.rootDir.path}/secret.properties"))

        target.buildCache {
            local.directory = File("${System.getProperty("user.home")}/build-cache-test")
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

    companion object {
        const val USE_REMOTE_BUILD_CACHE_LOCALLY = "use_remote_build_cache_locally"
    }
}
