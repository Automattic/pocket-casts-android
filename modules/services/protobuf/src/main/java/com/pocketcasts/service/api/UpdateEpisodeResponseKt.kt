// Generated by the protocol buffer compiler. DO NOT EDIT!
// source: api.proto

// Generated files should ignore deprecation warnings
@file:Suppress("DEPRECATION")
package com.pocketcasts.service.api

@kotlin.jvm.JvmName("-initializeupdateEpisodeResponse")
public inline fun updateEpisodeResponse(block: com.pocketcasts.service.api.UpdateEpisodeResponseKt.Dsl.() -> kotlin.Unit): com.pocketcasts.service.api.UpdateEpisodeResponse =
    com.pocketcasts.service.api.UpdateEpisodeResponseKt.Dsl._create(com.pocketcasts.service.api.UpdateEpisodeResponse.newBuilder()).apply { block() }._build()
/**
 * Protobuf type `api.UpdateEpisodeResponse`
 */
public object UpdateEpisodeResponseKt {
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    @com.google.protobuf.kotlin.ProtoDslMarker
    public class Dsl private constructor(
        private val _builder: com.pocketcasts.service.api.UpdateEpisodeResponse.Builder
    ) {
        public companion object {
            @kotlin.jvm.JvmSynthetic
            @kotlin.PublishedApi
            internal fun _create(builder: com.pocketcasts.service.api.UpdateEpisodeResponse.Builder): Dsl = Dsl(builder)
        }

        @kotlin.jvm.JvmSynthetic
        @kotlin.PublishedApi
        internal fun _build(): com.pocketcasts.service.api.UpdateEpisodeResponse = _builder.build()
    }
}
public inline fun com.pocketcasts.service.api.UpdateEpisodeResponse.copy(block: com.pocketcasts.service.api.UpdateEpisodeResponseKt.Dsl.() -> kotlin.Unit): com.pocketcasts.service.api.UpdateEpisodeResponse =
    com.pocketcasts.service.api.UpdateEpisodeResponseKt.Dsl._create(this.toBuilder()).apply { block() }._build()
