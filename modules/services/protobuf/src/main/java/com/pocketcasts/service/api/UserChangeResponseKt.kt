// Generated by the protocol buffer compiler. DO NOT EDIT!
// source: api.proto

// Generated files should ignore deprecation warnings
@file:Suppress("DEPRECATION")
package com.pocketcasts.service.api

@kotlin.jvm.JvmName("-initializeuserChangeResponse")
public inline fun userChangeResponse(block: com.pocketcasts.service.api.UserChangeResponseKt.Dsl.() -> kotlin.Unit): com.pocketcasts.service.api.UserChangeResponse =
    com.pocketcasts.service.api.UserChangeResponseKt.Dsl._create(com.pocketcasts.service.api.UserChangeResponse.newBuilder()).apply { block() }._build()
/**
 * Protobuf type `api.UserChangeResponse`
 */
public object UserChangeResponseKt {
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    @com.google.protobuf.kotlin.ProtoDslMarker
    public class Dsl private constructor(
        private val _builder: com.pocketcasts.service.api.UserChangeResponse.Builder
    ) {
        public companion object {
            @kotlin.jvm.JvmSynthetic
            @kotlin.PublishedApi
            internal fun _create(builder: com.pocketcasts.service.api.UserChangeResponse.Builder): Dsl = Dsl(builder)
        }

        @kotlin.jvm.JvmSynthetic
        @kotlin.PublishedApi
        internal fun _build(): com.pocketcasts.service.api.UserChangeResponse = _builder.build()

        /**
         * `.google.protobuf.BoolValue success = 1;`
         */
        public var success: com.google.protobuf.BoolValue
            @JvmName("getSuccess")
            get() = _builder.getSuccess()
            @JvmName("setSuccess")
            set(value) {
                _builder.setSuccess(value)
            }
        /**
         * `.google.protobuf.BoolValue success = 1;`
         */
        public fun clearSuccess() {
            _builder.clearSuccess()
        }
        /**
         * `.google.protobuf.BoolValue success = 1;`
         * @return Whether the success field is set.
         */
        public fun hasSuccess(): kotlin.Boolean {
            return _builder.hasSuccess()
        }

        /**
         * `string message = 2;`
         */
        public var message: kotlin.String
            @JvmName("getMessage")
            get() = _builder.getMessage()
            @JvmName("setMessage")
            set(value) {
                _builder.setMessage(value)
            }
        /**
         * `string message = 2;`
         */
        public fun clearMessage() {
            _builder.clearMessage()
        }

        /**
         * `string messageId = 3;`
         */
        public var messageId: kotlin.String
            @JvmName("getMessageId")
            get() = _builder.getMessageId()
            @JvmName("setMessageId")
            set(value) {
                _builder.setMessageId(value)
            }
        /**
         * `string messageId = 3;`
         */
        public fun clearMessageId() {
            _builder.clearMessageId()
        }
    }
}
public inline fun com.pocketcasts.service.api.UserChangeResponse.copy(block: com.pocketcasts.service.api.UserChangeResponseKt.Dsl.() -> kotlin.Unit): com.pocketcasts.service.api.UserChangeResponse =
    com.pocketcasts.service.api.UserChangeResponseKt.Dsl._create(this.toBuilder()).apply { block() }._build()

public val com.pocketcasts.service.api.UserChangeResponseOrBuilder.successOrNull: com.google.protobuf.BoolValue?
    get() = if (hasSuccess()) getSuccess() else null
