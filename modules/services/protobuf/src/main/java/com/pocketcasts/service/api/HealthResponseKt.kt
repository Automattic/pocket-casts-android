// Generated by the protocol buffer compiler. DO NOT EDIT!
// source: api.proto

// Generated files should ignore deprecation warnings
@file:Suppress("DEPRECATION")
package com.pocketcasts.service.api

@kotlin.jvm.JvmName("-initializehealthResponse")
public inline fun healthResponse(block: com.pocketcasts.service.api.HealthResponseKt.Dsl.() -> kotlin.Unit): com.pocketcasts.service.api.HealthResponse =
    com.pocketcasts.service.api.HealthResponseKt.Dsl._create(com.pocketcasts.service.api.HealthResponse.newBuilder()).apply { block() }._build()
/**
 * Protobuf type `api.HealthResponse`
 */
public object HealthResponseKt {
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    @com.google.protobuf.kotlin.ProtoDslMarker
    public class Dsl private constructor(
        private val _builder: com.pocketcasts.service.api.HealthResponse.Builder
    ) {
        public companion object {
            @kotlin.jvm.JvmSynthetic
            @kotlin.PublishedApi
            internal fun _create(builder: com.pocketcasts.service.api.HealthResponse.Builder): Dsl = Dsl(builder)
        }

        @kotlin.jvm.JvmSynthetic
        @kotlin.PublishedApi
        internal fun _build(): com.pocketcasts.service.api.HealthResponse = _builder.build()

        /**
         * <code>bool ok = 1;</code>
         */
        public var ok: kotlin.Boolean
            @JvmName("getOk")
            get() = _builder.getOk()
            @JvmName("setOk")
            set(value) {
                _builder.setOk(value)
            }
        /**
         * `bool ok = 1;`
         */
        public fun clearOk() {
            _builder.clearOk()
        }

        /**
         * An uninstantiable, behaviorless type to represent the field in
         * generics.
         */
        @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
        public class MessagesProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
        /**
         * `repeated string messages = 2;`
         * @return A list containing the messages.
         */
        public val messages: com.google.protobuf.kotlin.DslList<kotlin.String, MessagesProxy>
            @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
            get() = com.google.protobuf.kotlin.DslList(
                _builder.getMessagesList()
            )
        /**
         * `repeated string messages = 2;`
         * @param value The messages to add.
         */
        @kotlin.jvm.JvmSynthetic
        @kotlin.jvm.JvmName("addMessages")
        public fun com.google.protobuf.kotlin.DslList<kotlin.String, MessagesProxy>.add(value: kotlin.String) {
            _builder.addMessages(value)
        }
        /**
         * `repeated string messages = 2;`
         * @param value The messages to add.
         */
        @kotlin.jvm.JvmSynthetic
        @kotlin.jvm.JvmName("plusAssignMessages")
        @Suppress("NOTHING_TO_INLINE")
        public inline operator fun com.google.protobuf.kotlin.DslList<kotlin.String, MessagesProxy>.plusAssign(value: kotlin.String) {
            add(value)
        }
        /**
         * `repeated string messages = 2;`
         * @param values The messages to add.
         */
        @kotlin.jvm.JvmSynthetic
        @kotlin.jvm.JvmName("addAllMessages")
        public fun com.google.protobuf.kotlin.DslList<kotlin.String, MessagesProxy>.addAll(values: kotlin.collections.Iterable<kotlin.String>) {
            _builder.addAllMessages(values)
        }
        /**
         * `repeated string messages = 2;`
         * @param values The messages to add.
         */
        @kotlin.jvm.JvmSynthetic
        @kotlin.jvm.JvmName("plusAssignAllMessages")
        @Suppress("NOTHING_TO_INLINE")
        public inline operator fun com.google.protobuf.kotlin.DslList<kotlin.String, MessagesProxy>.plusAssign(values: kotlin.collections.Iterable<kotlin.String>) {
            addAll(values)
        }
        /**
         * `repeated string messages = 2;`
         * @param index The index to set the value at.
         * @param value The messages to set.
         */
        @kotlin.jvm.JvmSynthetic
        @kotlin.jvm.JvmName("setMessages")
        public operator fun com.google.protobuf.kotlin.DslList<kotlin.String, MessagesProxy>.set(index: kotlin.Int, value: kotlin.String) {
            _builder.setMessages(index, value)
        } /**
         * `repeated string messages = 2;`
         */
        @kotlin.jvm.JvmSynthetic
        @kotlin.jvm.JvmName("clearMessages")
        public fun com.google.protobuf.kotlin.DslList<kotlin.String, MessagesProxy>.clear() {
            _builder.clearMessages()
        }
    }
}
public inline fun com.pocketcasts.service.api.HealthResponse.copy(block: com.pocketcasts.service.api.HealthResponseKt.Dsl.() -> kotlin.Unit): com.pocketcasts.service.api.HealthResponse =
    com.pocketcasts.service.api.HealthResponseKt.Dsl._create(this.toBuilder()).apply { block() }._build()
