package au.com.shiftyjelly.pocketcasts.utils

import com.automattic.eventhorizon.DeviceType

enum class AppPlatform(
    val analyticsValue: DeviceType,
) {
    Automotive(
        analyticsValue = DeviceType.Car,
    ),
    Phone(
        analyticsValue = DeviceType.Phone,
    ),
    WearOs(
        analyticsValue = DeviceType.Watch,
    ),
}
