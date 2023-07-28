// Generated by the protocol buffer compiler. DO NOT EDIT!
// source: api.proto

// Generated files should ignore deprecation warnings
@file:Suppress("DEPRECATION")
package com.pocketcasts.service.api

@kotlin.jvm.JvmName("-initializeupNextRemoveRequest")
public inline fun upNextRemoveRequest(block: com.pocketcasts.service.api.UpNextRemoveRequestKt.Dsl.() -> kotlin.Unit): com.pocketcasts.service.api.UpNextRemoveRequest =
    com.pocketcasts.service.api.UpNextRemoveRequestKt.Dsl._create(com.pocketcasts.service.api.UpNextRemoveRequest.newBuilder()).apply { block() }._build()
/**
 * Protobuf type `api.UpNextRemoveRequest`
 */
public object UpNextRemoveRequestKt {
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    @com.google.protobuf.kotlin.ProtoDslMarker
    public class Dsl private constructor(
        private val _builder: com.pocketcasts.service.api.UpNextRemoveRequest.Builder
    ) {
        public companion object {
            @kotlin.jvm.JvmSynthetic
            @kotlin.PublishedApi
            internal fun _create(builder: com.pocketcasts.service.api.UpNextRemoveRequest.Builder): Dsl = Dsl(builder)
        }

        @kotlin.jvm.JvmSynthetic
        @kotlin.PublishedApi
        internal fun _build(): com.pocketcasts.service.api.UpNextRemoveRequest = _builder.build()

        /**
         * An uninstantiable, behaviorless type to represent the field in
         * generics.
         */
        @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
        public class UuidsProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
        /**
         * `repeated string uuids = 1;`
         * @return A list containing the uuids.
         */
        public val uuids: com.google.protobuf.kotlin.DslList<kotlin.String, UuidsProxy>
            @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
            get() = com.google.protobuf.kotlin.DslList(
                _builder.getUuidsList()
            )
        /**
         * `repeated string uuids = 1;`
         * @param value The uuids to add.
         */
        @kotlin.jvm.JvmSynthetic
        @kotlin.jvm.JvmName("addUuids")
        public fun com.google.protobuf.kotlin.DslList<kotlin.String, UuidsProxy>.add(value: kotlin.String) {
            _builder.addUuids(value)
        }
        /**
         * `repeated string uuids = 1;`
         * @param value The uuids to add.
         */
        @kotlin.jvm.JvmSynthetic
        @kotlin.jvm.JvmName("plusAssignUuids")
        @Suppress("NOTHING_TO_INLINE")
        public inline operator fun com.google.protobuf.kotlin.DslList<kotlin.String, UuidsProxy>.plusAssign(value: kotlin.String) {
            add(value)
        }
        /**
         * `repeated string uuids = 1;`
         * @param values The uuids to add.
         */
        @kotlin.jvm.JvmSynthetic
        @kotlin.jvm.JvmName("addAllUuids")
        public fun com.google.protobuf.kotlin.DslList<kotlin.String, UuidsProxy>.addAll(values: kotlin.collections.Iterable<kotlin.String>) {
            _builder.addAllUuids(values)
        }
        /**
         * `repeated string uuids = 1;`
         * @param values The uuids to add.
         */
        @kotlin.jvm.JvmSynthetic
        @kotlin.jvm.JvmName("plusAssignAllUuids")
        @Suppress("NOTHING_TO_INLINE")
        public inline operator fun com.google.protobuf.kotlin.DslList<kotlin.String, UuidsProxy>.plusAssign(values: kotlin.collections.Iterable<kotlin.String>) {
            addAll(values)
        }
        /**
         * `repeated string uuids = 1;`
         * @param index The index to set the value at.
         * @param value The uuids to set.
         */
        @kotlin.jvm.JvmSynthetic
        @kotlin.jvm.JvmName("setUuids")
        public operator fun com.google.protobuf.kotlin.DslList<kotlin.String, UuidsProxy>.set(index: kotlin.Int, value: kotlin.String) {
            _builder.setUuids(index, value)
        } /**
         * `repeated string uuids = 1;`
         */
        @kotlin.jvm.JvmSynthetic
        @kotlin.jvm.JvmName("clearUuids")
        public fun com.google.protobuf.kotlin.DslList<kotlin.String, UuidsProxy>.clear() {
            _builder.clearUuids()
        }
        /**
         * `string version = 2;`
         */
        public var version: kotlin.String
            @JvmName("getVersion")
            get() = _builder.getVersion()
            @JvmName("setVersion")
            set(value) {
                _builder.setVersion(value)
            }
        /**
         * `string version = 2;`
         */
        public fun clearVersion() {
            _builder.clearVersion()
        }
    }
}
public inline fun com.pocketcasts.service.api.UpNextRemoveRequest.copy(block: com.pocketcasts.service.api.UpNextRemoveRequestKt.Dsl.() -> kotlin.Unit): com.pocketcasts.service.api.UpNextRemoveRequest =
    com.pocketcasts.service.api.UpNextRemoveRequestKt.Dsl._create(this.toBuilder()).apply { block() }._build()
