package au.com.shiftyjelly.pocketcasts.analytics

data class EpisodeDownloadError(
    var reason: Reason = Reason.Unknown,
    var episodeUuid: String? = null,
    var podcastUuid: String? = null,
    var taskDuration: Long? = null,
    var httpStatusCode: Int? = null,
    var contentType: String? = null,
    var expectedContentLength: Long? = null,
    var responseBodyBytesReceived: Long? = null,
    var fileSize: Long? = null,
    var tlsCipherSuite: String? = null,
    var isCellular: Boolean? = null,
    var isProxy: Boolean? = null,
) {
    fun toProperties() = buildMap<String, Any> {
        put(REASON_KEY, reason.analyticsValue)
        episodeUuid?.let { put(EPISODE_UUID_KEY, it) }
        podcastUuid?.let { put(PODCAST_UUID_KEY, it) }
        taskDuration?.let { put(TASK_DURATION_KEY, it) }
        httpStatusCode?.let { put(HTTP_STATUS_CODE_KEY, it) }
        contentType?.let { put(CONTENT_TYPE_KEY, it) }
        expectedContentLength?.let { put(EXPECTED_CONTENT_LENGTH_KEY, it) }
        responseBodyBytesReceived?.let { put(RESPONSE_BODY_BYTES_RECEIVED_KEY, it) }
        fileSize?.let { put(FILE_SIZE_KEY, it) }
        tlsCipherSuite?.let { put(TLS_CIPHER_SUITE_KEY, it) }
        isCellular?.let { put(IS_CELLULAR_KEY, it) }
        isProxy?.let { put(IS_PROXY_KEY, it) }
    }

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
        const val CONTENT_TYPE_KEY = "http_content_type"
        const val EXPECTED_CONTENT_LENGTH_KEY = "expected_content_length"
        const val RESPONSE_BODY_BYTES_RECEIVED_KEY = "response_body_bytes_received"
        const val FILE_SIZE_KEY = "file_size_key"
        const val TLS_CIPHER_SUITE_KEY = "tls_cipher_suite"
        const val IS_CELLULAR_KEY = "is_cellular"
        const val IS_PROXY_KEY = "is_proxy"

        fun fromProperties(properties: Map<String, Any?>) = EpisodeDownloadError(
            reason = properties.find<String>(REASON_KEY)?.let { reason -> Reason.entries.find { it.analyticsValue == reason } } ?: Reason.Unknown,
            episodeUuid = properties.find<String>(EPISODE_UUID_KEY),
            podcastUuid = properties.find<String>(PODCAST_UUID_KEY),
            taskDuration = properties.find<Long>(TASK_DURATION_KEY),
            httpStatusCode = properties.find<Int>(HTTP_STATUS_CODE_KEY),
            contentType = properties.find<String>(CONTENT_TYPE_KEY),
            expectedContentLength = properties.find<Long>(EXPECTED_CONTENT_LENGTH_KEY),
            responseBodyBytesReceived = properties.find<Long>(RESPONSE_BODY_BYTES_RECEIVED_KEY),
            fileSize = properties.find<Long>(FILE_SIZE_KEY),
            tlsCipherSuite = properties.find<String>(TLS_CIPHER_SUITE_KEY),
            isCellular = properties.find<Boolean>(IS_CELLULAR_KEY),
            isProxy = properties.find<Boolean>(IS_PROXY_KEY),
        )

        private inline fun <reified T> Map<String, Any?>.find(key: String) = get(key) as? T
    }
}
