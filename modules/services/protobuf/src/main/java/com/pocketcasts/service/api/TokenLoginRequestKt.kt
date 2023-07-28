// Generated by the protocol buffer compiler. DO NOT EDIT!
// source: api.proto

// Generated files should ignore deprecation warnings
@file:Suppress("DEPRECATION")
package com.pocketcasts.service.api

@kotlin.jvm.JvmName("-initializetokenLoginRequest")
public inline fun tokenLoginRequest(block: com.pocketcasts.service.api.TokenLoginRequestKt.Dsl.() -> kotlin.Unit): com.pocketcasts.service.api.TokenLoginRequest =
    com.pocketcasts.service.api.TokenLoginRequestKt.Dsl._create(com.pocketcasts.service.api.TokenLoginRequest.newBuilder()).apply { block() }._build()
/**
 * Protobuf type `api.TokenLoginRequest`
 */
public object TokenLoginRequestKt {
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    @com.google.protobuf.kotlin.ProtoDslMarker
    public class Dsl private constructor(
        private val _builder: com.pocketcasts.service.api.TokenLoginRequest.Builder
    ) {
        public companion object {
            @kotlin.jvm.JvmSynthetic
            @kotlin.PublishedApi
            internal fun _create(builder: com.pocketcasts.service.api.TokenLoginRequest.Builder): Dsl = Dsl(builder)
        }

        @kotlin.jvm.JvmSynthetic
        @kotlin.PublishedApi
        internal fun _build(): com.pocketcasts.service.api.TokenLoginRequest = _builder.build()

        /**
         * `string id_token = 1;`
         */
        public var idToken: kotlin.String
            @JvmName("getIdToken")
            get() = _builder.getIdToken()
            @JvmName("setIdToken")
            set(value) {
                _builder.setIdToken(value)
            }
        /**
         * `string id_token = 1;`
         */
        public fun clearIdToken() {
            _builder.clearIdToken()
        }

        /**
         * `string email = 2;`
         */
        public var email: kotlin.String
            @JvmName("getEmail")
            get() = _builder.getEmail()
            @JvmName("setEmail")
            set(value) {
                _builder.setEmail(value)
            }
        /**
         * `string email = 2;`
         */
        public fun clearEmail() {
            _builder.clearEmail()
        }

        /**
         * `string password = 3;`
         */
        public var password: kotlin.String
            @JvmName("getPassword")
            get() = _builder.getPassword()
            @JvmName("setPassword")
            set(value) {
                _builder.setPassword(value)
            }
        /**
         * `string password = 3;`
         */
        public fun clearPassword() {
            _builder.clearPassword()
        }

        /**
         * `string scope = 4;`
         */
        public var scope: kotlin.String
            @JvmName("getScope")
            get() = _builder.getScope()
            @JvmName("setScope")
            set(value) {
                _builder.setScope(value)
            }
        /**
         * `string scope = 4;`
         */
        public fun clearScope() {
            _builder.clearScope()
        }
    }
}
public inline fun com.pocketcasts.service.api.TokenLoginRequest.copy(block: com.pocketcasts.service.api.TokenLoginRequestKt.Dsl.() -> kotlin.Unit): com.pocketcasts.service.api.TokenLoginRequest =
    com.pocketcasts.service.api.TokenLoginRequestKt.Dsl._create(this.toBuilder()).apply { block() }._build()
