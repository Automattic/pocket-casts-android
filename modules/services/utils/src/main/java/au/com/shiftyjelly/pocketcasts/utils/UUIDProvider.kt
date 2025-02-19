package au.com.shiftyjelly.pocketcasts.utils

import java.util.UUID

interface UUIDProvider {
    fun generateUUID(): UUID
}
