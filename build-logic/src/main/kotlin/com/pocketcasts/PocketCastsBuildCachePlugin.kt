package com.pocketcasts

import java.net.URI
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.caching.http.HttpBuildCache
import org.gradle.kotlin.dsl.remote

class PocketCastsBuildCachePlugin : Plugin<Settings> {

    override fun apply(target: Settings) {
        if (System.getenv("CI")?.toBoolean() == true) {
            target.buildCache {
                remote<HttpBuildCache> {
                    url = URI.create("http://10.0.2.215:5071/cache/")
                    isAllowUntrustedServer = true
                    isAllowInsecureProtocol = true
                    isPush = true
                }
            }
        }
    }
}
