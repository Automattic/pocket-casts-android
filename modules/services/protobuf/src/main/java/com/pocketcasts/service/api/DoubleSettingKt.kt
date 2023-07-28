// Generated by the protocol buffer compiler. DO NOT EDIT!
// source: api.proto

// Generated files should ignore deprecation warnings
@file:Suppress("DEPRECATION")
package com.pocketcasts.service.api

@kotlin.jvm.JvmName("-initializedoubleSetting")
public inline fun doubleSetting(block: com.pocketcasts.service.api.DoubleSettingKt.Dsl.() -> kotlin.Unit): com.pocketcasts.service.api.DoubleSetting =
    com.pocketcasts.service.api.DoubleSettingKt.Dsl._create(com.pocketcasts.service.api.DoubleSetting.newBuilder()).apply { block() }._build()
/**
 * Protobuf type `api.DoubleSetting`
 */
public object DoubleSettingKt {
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    @com.google.protobuf.kotlin.ProtoDslMarker
    public class Dsl private constructor(
        private val _builder: com.pocketcasts.service.api.DoubleSetting.Builder
    ) {
        public companion object {
            @kotlin.jvm.JvmSynthetic
            @kotlin.PublishedApi
            internal fun _create(builder: com.pocketcasts.service.api.DoubleSetting.Builder): Dsl = Dsl(builder)
        }

        @kotlin.jvm.JvmSynthetic
        @kotlin.PublishedApi
        internal fun _build(): com.pocketcasts.service.api.DoubleSetting = _builder.build()

        /**
         * `.google.protobuf.DoubleValue value = 1;`
         */
        public var value: com.google.protobuf.DoubleValue
            @JvmName("getValue")
            get() = _builder.getValue()
            @JvmName("setValue")
            set(value) {
                _builder.setValue(value)
            }
        /**
         * `.google.protobuf.DoubleValue value = 1;`
         */
        public fun clearValue() {
            _builder.clearValue()
        }
        /**
         * `.google.protobuf.DoubleValue value = 1;`
         * @return Whether the value field is set.
         */
        public fun hasValue(): kotlin.Boolean {
            return _builder.hasValue()
        }

        /**
         * `.google.protobuf.BoolValue changed = 2;`
         */
        public var changed: com.google.protobuf.BoolValue
            @JvmName("getChanged")
            get() = _builder.getChanged()
            @JvmName("setChanged")
            set(value) {
                _builder.setChanged(value)
            }
        /**
         * `.google.protobuf.BoolValue changed = 2;`
         */
        public fun clearChanged() {
            _builder.clearChanged()
        }
        /**
         * `.google.protobuf.BoolValue changed = 2;`
         * @return Whether the changed field is set.
         */
        public fun hasChanged(): kotlin.Boolean {
            return _builder.hasChanged()
        }
    }
}
public inline fun com.pocketcasts.service.api.DoubleSetting.copy(block: com.pocketcasts.service.api.DoubleSettingKt.Dsl.() -> kotlin.Unit): com.pocketcasts.service.api.DoubleSetting =
    com.pocketcasts.service.api.DoubleSettingKt.Dsl._create(this.toBuilder()).apply { block() }._build()

public val com.pocketcasts.service.api.DoubleSettingOrBuilder.valueOrNull: com.google.protobuf.DoubleValue?
    get() = if (hasValue()) getValue() else null

public val com.pocketcasts.service.api.DoubleSettingOrBuilder.changedOrNull: com.google.protobuf.BoolValue?
    get() = if (hasChanged()) getChanged() else null
