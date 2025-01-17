// Only run build cache on CI builds.
if (System.getenv("CI") != null) {
    buildCache {
        remote(HttpBuildCache::class) {
            url = uri("http://10.0.2.215:5071/cache/")
            isAllowUntrustedServer = true
            isAllowInsecureProtocol = true
            isPush = true
        }
    }
}
