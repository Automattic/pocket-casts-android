package au.com.shiftyjelly.pocketcasts.utils

import java.util.UUID

class UUIDProviderImpl : UUIDProvider {
    override fun generateUUID(): UUID = UUID.randomUUID()
}
