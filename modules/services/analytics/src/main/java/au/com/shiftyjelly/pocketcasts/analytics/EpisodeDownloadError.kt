package au.com.shiftyjelly.pocketcasts.analytics

data class EpisodeDownloadError(
    var reason: Reason = Reason.Unknown,
    var episodeUuid: String = "",
    var podcastUuid: String = "",
    var taskDuration: Long = -1,
    var httpStatusCode: Int = -1,
    var contentType: String = "",
    var expectedContentLength: Long = -1,
    var responseBodyBytesReceived: Long = -1,
    var tlsCipherSuite: String = "",
    var isCellular: Boolean = false,
    var isProxy: Boolean = false,
) {

    fun toProperties() = mapOf(
        REASON_KEY to reason.analyticsValue,
        EPISODE_UUID_KEY to episodeUuid,
        PODCAST_UUID_KEY to podcastUuid,
        TASK_DURATION_KEY to taskDuration,
        HTTP_STATUS_CODE_KEY to httpStatusCode,
        CONTENT_TYPE_KEY to contentType,
        EXPECTED_CONTENT_LENGTH_KEY to expectedContentLength,
        RESPONSE_BODY_BYTES_RECEIVED_KEY to responseBodyBytesReceived,
        TLS_CIPHER_SUITE_KEY to tlsCipherSuite,
        IS_CELLULAR_KEY to isCellular,
        IS_PROXY_KEY to isProxy,
    )

    enum class Reason(
        val analyticsValue: String,
    ) {
        Unknown("unknown"),
        TaskIdMismatch("task_id_mismatch"),
        MalformedHost("malformed_host"),
        UnknownHost("unknown_host"),
        ConnectionTimeout("connection_timeout"),
        SocketIssue("socket_issue"),
        NoSSl("no_ssl"),
        ChartableBlocked("chartable_blocked"),
        StatusCode("status_code"),
        ContentType("content_type"),
        SuspiciousContent("suspicious_content"),
        PartialDownload("partial_download"),
        NotEnoughStorage("not_enough_storage"),
        NoSavePath("no_save_path"),
        StorageIssue("storage_issue"),
    }

    companion object {
        const val REASON_KEY = "reason"
        const val EPISODE_UUID_KEY = "episode_uuid"
        const val PODCAST_UUID_KEY = "podcast_uuid"
        const val TASK_DURATION_KEY = "duration"
        const val HTTP_STATUS_CODE_KEY = "http_status_code"
        const val CONTENT_TYPE_KEY = "content_type"
        const val EXPECTED_CONTENT_LENGTH_KEY = "expected_content_length"
        const val RESPONSE_BODY_BYTES_RECEIVED_KEY = "response_body_bytes_received"
        const val TLS_CIPHER_SUITE_KEY = "tls_cipher_suite"
        const val IS_CELLULAR_KEY = "is_cellular"
        const val IS_PROXY_KEY = "is_proxy"

        fun fromProperties(properties: Map<String, Any>) = EpisodeDownloadError(
            reason = properties.require<String>(REASON_KEY).let { reason -> Reason.entries.single { it.analyticsValue == reason } },
            episodeUuid = properties.require<String>(EPISODE_UUID_KEY),
            podcastUuid = properties.require<String>(PODCAST_UUID_KEY),
            taskDuration = properties.require<Long>(TASK_DURATION_KEY),
            httpStatusCode = properties.require<Int>(HTTP_STATUS_CODE_KEY),
            contentType = properties.require<String>(CONTENT_TYPE_KEY),
            expectedContentLength = properties.require<Long>(EXPECTED_CONTENT_LENGTH_KEY),
            responseBodyBytesReceived = properties.require<Long>(RESPONSE_BODY_BYTES_RECEIVED_KEY),
            tlsCipherSuite = properties.require<String>(TLS_CIPHER_SUITE_KEY),
            isCellular = properties.require<Boolean>(IS_CELLULAR_KEY),
            isProxy = properties.require<Boolean>(IS_PROXY_KEY),
        )

        private inline fun <reified T> Map<String, Any>.require(key: String) = requireNotNull(get(key) as T) {
            "Missing property '$key' of type '${T::class.java}'"
        }
    }
}
