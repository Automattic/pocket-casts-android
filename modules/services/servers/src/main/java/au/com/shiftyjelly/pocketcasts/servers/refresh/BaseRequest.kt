package au.com.shiftyjelly.pocketcasts.servers.refresh

abstract class BaseRequest {
    abstract var deviceId: String?
    abstract var datetime: String?
    abstract var version: String?
    abstract var appVersion: String?
    abstract var appVersionCode: String?
    abstract var hash: String?
    abstract var deviceType: String?
    abstract var country: String?
    abstract var language: String?
    abstract var model: String?
}
