// Generated by the protocol buffer compiler. DO NOT EDIT!
// source: api.proto

// Generated files should ignore deprecation warnings
@file:Suppress("DEPRECATION")
package com.pocketcasts.service.api

@kotlin.jvm.JvmName("-initializesyncUserDevice")
public inline fun syncUserDevice(block: com.pocketcasts.service.api.SyncUserDeviceKt.Dsl.() -> kotlin.Unit): com.pocketcasts.service.api.SyncUserDevice =
    com.pocketcasts.service.api.SyncUserDeviceKt.Dsl._create(com.pocketcasts.service.api.SyncUserDevice.newBuilder()).apply { block() }._build()
/**
 * Protobuf type `api.SyncUserDevice`
 */
public object SyncUserDeviceKt {
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    @com.google.protobuf.kotlin.ProtoDslMarker
    public class Dsl private constructor(
        private val _builder: com.pocketcasts.service.api.SyncUserDevice.Builder
    ) {
        public companion object {
            @kotlin.jvm.JvmSynthetic
            @kotlin.PublishedApi
            internal fun _create(builder: com.pocketcasts.service.api.SyncUserDevice.Builder): Dsl = Dsl(builder)
        }

        @kotlin.jvm.JvmSynthetic
        @kotlin.PublishedApi
        internal fun _build(): com.pocketcasts.service.api.SyncUserDevice = _builder.build()

        /**
         * `.google.protobuf.StringValue device_id = 1;`
         */
        public var deviceId: com.google.protobuf.StringValue
            @JvmName("getDeviceId")
            get() = _builder.getDeviceId()
            @JvmName("setDeviceId")
            set(value) {
                _builder.setDeviceId(value)
            }
        /**
         * `.google.protobuf.StringValue device_id = 1;`
         */
        public fun clearDeviceId() {
            _builder.clearDeviceId()
        }
        /**
         * `.google.protobuf.StringValue device_id = 1;`
         * @return Whether the deviceId field is set.
         */
        public fun hasDeviceId(): kotlin.Boolean {
            return _builder.hasDeviceId()
        }

        /**
         * `.google.protobuf.Int32Value device_type = 2;`
         */
        public var deviceType: com.google.protobuf.Int32Value
            @JvmName("getDeviceType")
            get() = _builder.getDeviceType()
            @JvmName("setDeviceType")
            set(value) {
                _builder.setDeviceType(value)
            }
        /**
         * `.google.protobuf.Int32Value device_type = 2;`
         */
        public fun clearDeviceType() {
            _builder.clearDeviceType()
        }
        /**
         * `.google.protobuf.Int32Value device_type = 2;`
         * @return Whether the deviceType field is set.
         */
        public fun hasDeviceType(): kotlin.Boolean {
            return _builder.hasDeviceType()
        }

        /**
         * ```
         * times in seconds
         * ```
         *
         * `.google.protobuf.Int64Value times_started_at = 3;`
         */
        public var timesStartedAt: com.google.protobuf.Int64Value
            @JvmName("getTimesStartedAt")
            get() = _builder.getTimesStartedAt()
            @JvmName("setTimesStartedAt")
            set(value) {
                _builder.setTimesStartedAt(value)
            }
        /**
         * ```
         * times in seconds
         * ```
         *
         * `.google.protobuf.Int64Value times_started_at = 3;`
         */
        public fun clearTimesStartedAt() {
            _builder.clearTimesStartedAt()
        }
        /**
         * ```
         * times in seconds
         * ```
         *
         * `.google.protobuf.Int64Value times_started_at = 3;`
         * @return Whether the timesStartedAt field is set.
         */
        public fun hasTimesStartedAt(): kotlin.Boolean {
            return _builder.hasTimesStartedAt()
        }

        /**
         * `.google.protobuf.Int64Value time_silence_removal = 4;`
         */
        public var timeSilenceRemoval: com.google.protobuf.Int64Value
            @JvmName("getTimeSilenceRemoval")
            get() = _builder.getTimeSilenceRemoval()
            @JvmName("setTimeSilenceRemoval")
            set(value) {
                _builder.setTimeSilenceRemoval(value)
            }
        /**
         * `.google.protobuf.Int64Value time_silence_removal = 4;`
         */
        public fun clearTimeSilenceRemoval() {
            _builder.clearTimeSilenceRemoval()
        }
        /**
         * `.google.protobuf.Int64Value time_silence_removal = 4;`
         * @return Whether the timeSilenceRemoval field is set.
         */
        public fun hasTimeSilenceRemoval(): kotlin.Boolean {
            return _builder.hasTimeSilenceRemoval()
        }

        /**
         * `.google.protobuf.Int64Value time_variable_speed = 5;`
         */
        public var timeVariableSpeed: com.google.protobuf.Int64Value
            @JvmName("getTimeVariableSpeed")
            get() = _builder.getTimeVariableSpeed()
            @JvmName("setTimeVariableSpeed")
            set(value) {
                _builder.setTimeVariableSpeed(value)
            }
        /**
         * `.google.protobuf.Int64Value time_variable_speed = 5;`
         */
        public fun clearTimeVariableSpeed() {
            _builder.clearTimeVariableSpeed()
        }
        /**
         * `.google.protobuf.Int64Value time_variable_speed = 5;`
         * @return Whether the timeVariableSpeed field is set.
         */
        public fun hasTimeVariableSpeed(): kotlin.Boolean {
            return _builder.hasTimeVariableSpeed()
        }

        /**
         * `.google.protobuf.Int64Value time_intro_skipping = 6;`
         */
        public var timeIntroSkipping: com.google.protobuf.Int64Value
            @JvmName("getTimeIntroSkipping")
            get() = _builder.getTimeIntroSkipping()
            @JvmName("setTimeIntroSkipping")
            set(value) {
                _builder.setTimeIntroSkipping(value)
            }
        /**
         * `.google.protobuf.Int64Value time_intro_skipping = 6;`
         */
        public fun clearTimeIntroSkipping() {
            _builder.clearTimeIntroSkipping()
        }
        /**
         * `.google.protobuf.Int64Value time_intro_skipping = 6;`
         * @return Whether the timeIntroSkipping field is set.
         */
        public fun hasTimeIntroSkipping(): kotlin.Boolean {
            return _builder.hasTimeIntroSkipping()
        }

        /**
         * `.google.protobuf.Int64Value time_skipping = 7;`
         */
        public var timeSkipping: com.google.protobuf.Int64Value
            @JvmName("getTimeSkipping")
            get() = _builder.getTimeSkipping()
            @JvmName("setTimeSkipping")
            set(value) {
                _builder.setTimeSkipping(value)
            }
        /**
         * `.google.protobuf.Int64Value time_skipping = 7;`
         */
        public fun clearTimeSkipping() {
            _builder.clearTimeSkipping()
        }
        /**
         * `.google.protobuf.Int64Value time_skipping = 7;`
         * @return Whether the timeSkipping field is set.
         */
        public fun hasTimeSkipping(): kotlin.Boolean {
            return _builder.hasTimeSkipping()
        }

        /**
         * `.google.protobuf.Int64Value time_listened = 8;`
         */
        public var timeListened: com.google.protobuf.Int64Value
            @JvmName("getTimeListened")
            get() = _builder.getTimeListened()
            @JvmName("setTimeListened")
            set(value) {
                _builder.setTimeListened(value)
            }
        /**
         * `.google.protobuf.Int64Value time_listened = 8;`
         */
        public fun clearTimeListened() {
            _builder.clearTimeListened()
        }
        /**
         * `.google.protobuf.Int64Value time_listened = 8;`
         * @return Whether the timeListened field is set.
         */
        public fun hasTimeListened(): kotlin.Boolean {
            return _builder.hasTimeListened()
        }
    }
}
public inline fun com.pocketcasts.service.api.SyncUserDevice.copy(block: com.pocketcasts.service.api.SyncUserDeviceKt.Dsl.() -> kotlin.Unit): com.pocketcasts.service.api.SyncUserDevice =
    com.pocketcasts.service.api.SyncUserDeviceKt.Dsl._create(this.toBuilder()).apply { block() }._build()

public val com.pocketcasts.service.api.SyncUserDeviceOrBuilder.deviceIdOrNull: com.google.protobuf.StringValue?
    get() = if (hasDeviceId()) getDeviceId() else null

public val com.pocketcasts.service.api.SyncUserDeviceOrBuilder.deviceTypeOrNull: com.google.protobuf.Int32Value?
    get() = if (hasDeviceType()) getDeviceType() else null

public val com.pocketcasts.service.api.SyncUserDeviceOrBuilder.timesStartedAtOrNull: com.google.protobuf.Int64Value?
    get() = if (hasTimesStartedAt()) getTimesStartedAt() else null

public val com.pocketcasts.service.api.SyncUserDeviceOrBuilder.timeSilenceRemovalOrNull: com.google.protobuf.Int64Value?
    get() = if (hasTimeSilenceRemoval()) getTimeSilenceRemoval() else null

public val com.pocketcasts.service.api.SyncUserDeviceOrBuilder.timeVariableSpeedOrNull: com.google.protobuf.Int64Value?
    get() = if (hasTimeVariableSpeed()) getTimeVariableSpeed() else null

public val com.pocketcasts.service.api.SyncUserDeviceOrBuilder.timeIntroSkippingOrNull: com.google.protobuf.Int64Value?
    get() = if (hasTimeIntroSkipping()) getTimeIntroSkipping() else null

public val com.pocketcasts.service.api.SyncUserDeviceOrBuilder.timeSkippingOrNull: com.google.protobuf.Int64Value?
    get() = if (hasTimeSkipping()) getTimeSkipping() else null

public val com.pocketcasts.service.api.SyncUserDeviceOrBuilder.timeListenedOrNull: com.google.protobuf.Int64Value?
    get() = if (hasTimeListened()) getTimeListened() else null
