// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: api.proto

// Protobuf Java Version: 3.25.1
package com.pocketcasts.service.api;

/**
 * Protobuf type {@code api.SyncUpdateRequest}
 */
public  final class SyncUpdateRequest extends
    com.google.protobuf.GeneratedMessageLite<
        SyncUpdateRequest, SyncUpdateRequest.Builder> implements
    // @@protoc_insertion_point(message_implements:api.SyncUpdateRequest)
    SyncUpdateRequestOrBuilder {
  private SyncUpdateRequest() {
    country_ = "";
    deviceId_ = "";
    records_ = emptyProtobufList();
  }
  public static final int DEVICE_UTC_TIME_MS_FIELD_NUMBER = 1;
  private long deviceUtcTimeMs_;
  /**
   * <code>int64 device_utc_time_ms = 1;</code>
   * @return The deviceUtcTimeMs.
   */
  @java.lang.Override
  public long getDeviceUtcTimeMs() {
    return deviceUtcTimeMs_;
  }
  /**
   * <code>int64 device_utc_time_ms = 1;</code>
   * @param value The deviceUtcTimeMs to set.
   */
  private void setDeviceUtcTimeMs(long value) {
    
    deviceUtcTimeMs_ = value;
  }
  /**
   * <code>int64 device_utc_time_ms = 1;</code>
   */
  private void clearDeviceUtcTimeMs() {

    deviceUtcTimeMs_ = 0L;
  }

  public static final int LAST_MODIFIED_FIELD_NUMBER = 2;
  private long lastModified_;
  /**
   * <code>int64 last_modified = 2;</code>
   * @return The lastModified.
   */
  @java.lang.Override
  public long getLastModified() {
    return lastModified_;
  }
  /**
   * <code>int64 last_modified = 2;</code>
   * @param value The lastModified to set.
   */
  private void setLastModified(long value) {
    
    lastModified_ = value;
  }
  /**
   * <code>int64 last_modified = 2;</code>
   */
  private void clearLastModified() {

    lastModified_ = 0L;
  }

  public static final int COUNTRY_FIELD_NUMBER = 3;
  private java.lang.String country_;
  /**
   * <code>string country = 3;</code>
   * @return The country.
   */
  @java.lang.Override
  public java.lang.String getCountry() {
    return country_;
  }
  /**
   * <code>string country = 3;</code>
   * @return The bytes for country.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getCountryBytes() {
    return com.google.protobuf.ByteString.copyFromUtf8(country_);
  }
  /**
   * <code>string country = 3;</code>
   * @param value The country to set.
   */
  private void setCountry(
      java.lang.String value) {
    java.lang.Class<?> valueClass = value.getClass();
  
    country_ = value;
  }
  /**
   * <code>string country = 3;</code>
   */
  private void clearCountry() {

    country_ = getDefaultInstance().getCountry();
  }
  /**
   * <code>string country = 3;</code>
   * @param value The bytes for country to set.
   */
  private void setCountryBytes(
      com.google.protobuf.ByteString value) {
    checkByteStringIsUtf8(value);
    country_ = value.toStringUtf8();

  }

  public static final int DEVICE_ID_FIELD_NUMBER = 4;
  private java.lang.String deviceId_;
  /**
   * <code>string device_id = 4;</code>
   * @return The deviceId.
   */
  @java.lang.Override
  public java.lang.String getDeviceId() {
    return deviceId_;
  }
  /**
   * <code>string device_id = 4;</code>
   * @return The bytes for deviceId.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getDeviceIdBytes() {
    return com.google.protobuf.ByteString.copyFromUtf8(deviceId_);
  }
  /**
   * <code>string device_id = 4;</code>
   * @param value The deviceId to set.
   */
  private void setDeviceId(
      java.lang.String value) {
    java.lang.Class<?> valueClass = value.getClass();
  
    deviceId_ = value;
  }
  /**
   * <code>string device_id = 4;</code>
   */
  private void clearDeviceId() {

    deviceId_ = getDefaultInstance().getDeviceId();
  }
  /**
   * <code>string device_id = 4;</code>
   * @param value The bytes for deviceId to set.
   */
  private void setDeviceIdBytes(
      com.google.protobuf.ByteString value) {
    checkByteStringIsUtf8(value);
    deviceId_ = value.toStringUtf8();

  }

  public static final int RECORDS_FIELD_NUMBER = 5;
  private com.google.protobuf.Internal.ProtobufList<com.pocketcasts.service.api.Record> records_;
  /**
   * <code>repeated .api.Record records = 5;</code>
   */
  @java.lang.Override
  public java.util.List<com.pocketcasts.service.api.Record> getRecordsList() {
    return records_;
  }
  /**
   * <code>repeated .api.Record records = 5;</code>
   */
  public java.util.List<? extends com.pocketcasts.service.api.RecordOrBuilder> 
      getRecordsOrBuilderList() {
    return records_;
  }
  /**
   * <code>repeated .api.Record records = 5;</code>
   */
  @java.lang.Override
  public int getRecordsCount() {
    return records_.size();
  }
  /**
   * <code>repeated .api.Record records = 5;</code>
   */
  @java.lang.Override
  public com.pocketcasts.service.api.Record getRecords(int index) {
    return records_.get(index);
  }
  /**
   * <code>repeated .api.Record records = 5;</code>
   */
  public com.pocketcasts.service.api.RecordOrBuilder getRecordsOrBuilder(
      int index) {
    return records_.get(index);
  }
  private void ensureRecordsIsMutable() {
    com.google.protobuf.Internal.ProtobufList<com.pocketcasts.service.api.Record> tmp = records_;
    if (!tmp.isModifiable()) {
      records_ =
          com.google.protobuf.GeneratedMessageLite.mutableCopy(tmp);
     }
  }

  /**
   * <code>repeated .api.Record records = 5;</code>
   */
  private void setRecords(
      int index, com.pocketcasts.service.api.Record value) {
    value.getClass();
  ensureRecordsIsMutable();
    records_.set(index, value);
  }
  /**
   * <code>repeated .api.Record records = 5;</code>
   */
  private void addRecords(com.pocketcasts.service.api.Record value) {
    value.getClass();
  ensureRecordsIsMutable();
    records_.add(value);
  }
  /**
   * <code>repeated .api.Record records = 5;</code>
   */
  private void addRecords(
      int index, com.pocketcasts.service.api.Record value) {
    value.getClass();
  ensureRecordsIsMutable();
    records_.add(index, value);
  }
  /**
   * <code>repeated .api.Record records = 5;</code>
   */
  private void addAllRecords(
      java.lang.Iterable<? extends com.pocketcasts.service.api.Record> values) {
    ensureRecordsIsMutable();
    com.google.protobuf.AbstractMessageLite.addAll(
        values, records_);
  }
  /**
   * <code>repeated .api.Record records = 5;</code>
   */
  private void clearRecords() {
    records_ = emptyProtobufList();
  }
  /**
   * <code>repeated .api.Record records = 5;</code>
   */
  private void removeRecords(int index) {
    ensureRecordsIsMutable();
    records_.remove(index);
  }

  public static com.pocketcasts.service.api.SyncUpdateRequest parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.pocketcasts.service.api.SyncUpdateRequest parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.pocketcasts.service.api.SyncUpdateRequest parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.pocketcasts.service.api.SyncUpdateRequest parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.pocketcasts.service.api.SyncUpdateRequest parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.pocketcasts.service.api.SyncUpdateRequest parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.pocketcasts.service.api.SyncUpdateRequest parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input);
  }
  public static com.pocketcasts.service.api.SyncUpdateRequest parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input, extensionRegistry);
  }

  public static com.pocketcasts.service.api.SyncUpdateRequest parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return parseDelimitedFrom(DEFAULT_INSTANCE, input);
  }

  public static com.pocketcasts.service.api.SyncUpdateRequest parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return parseDelimitedFrom(DEFAULT_INSTANCE, input, extensionRegistry);
  }
  public static com.pocketcasts.service.api.SyncUpdateRequest parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input);
  }
  public static com.pocketcasts.service.api.SyncUpdateRequest parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input, extensionRegistry);
  }

  public static Builder newBuilder() {
    return (Builder) DEFAULT_INSTANCE.createBuilder();
  }
  public static Builder newBuilder(com.pocketcasts.service.api.SyncUpdateRequest prototype) {
    return DEFAULT_INSTANCE.createBuilder(prototype);
  }

  /**
   * Protobuf type {@code api.SyncUpdateRequest}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageLite.Builder<
        com.pocketcasts.service.api.SyncUpdateRequest, Builder> implements
      // @@protoc_insertion_point(builder_implements:api.SyncUpdateRequest)
      com.pocketcasts.service.api.SyncUpdateRequestOrBuilder {
    // Construct using com.pocketcasts.service.api.SyncUpdateRequest.newBuilder()
    private Builder() {
      super(DEFAULT_INSTANCE);
    }


    /**
     * <code>int64 device_utc_time_ms = 1;</code>
     * @return The deviceUtcTimeMs.
     */
    @java.lang.Override
    public long getDeviceUtcTimeMs() {
      return instance.getDeviceUtcTimeMs();
    }
    /**
     * <code>int64 device_utc_time_ms = 1;</code>
     * @param value The deviceUtcTimeMs to set.
     * @return This builder for chaining.
     */
    public Builder setDeviceUtcTimeMs(long value) {
      copyOnWrite();
      instance.setDeviceUtcTimeMs(value);
      return this;
    }
    /**
     * <code>int64 device_utc_time_ms = 1;</code>
     * @return This builder for chaining.
     */
    public Builder clearDeviceUtcTimeMs() {
      copyOnWrite();
      instance.clearDeviceUtcTimeMs();
      return this;
    }

    /**
     * <code>int64 last_modified = 2;</code>
     * @return The lastModified.
     */
    @java.lang.Override
    public long getLastModified() {
      return instance.getLastModified();
    }
    /**
     * <code>int64 last_modified = 2;</code>
     * @param value The lastModified to set.
     * @return This builder for chaining.
     */
    public Builder setLastModified(long value) {
      copyOnWrite();
      instance.setLastModified(value);
      return this;
    }
    /**
     * <code>int64 last_modified = 2;</code>
     * @return This builder for chaining.
     */
    public Builder clearLastModified() {
      copyOnWrite();
      instance.clearLastModified();
      return this;
    }

    /**
     * <code>string country = 3;</code>
     * @return The country.
     */
    @java.lang.Override
    public java.lang.String getCountry() {
      return instance.getCountry();
    }
    /**
     * <code>string country = 3;</code>
     * @return The bytes for country.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString
        getCountryBytes() {
      return instance.getCountryBytes();
    }
    /**
     * <code>string country = 3;</code>
     * @param value The country to set.
     * @return This builder for chaining.
     */
    public Builder setCountry(
        java.lang.String value) {
      copyOnWrite();
      instance.setCountry(value);
      return this;
    }
    /**
     * <code>string country = 3;</code>
     * @return This builder for chaining.
     */
    public Builder clearCountry() {
      copyOnWrite();
      instance.clearCountry();
      return this;
    }
    /**
     * <code>string country = 3;</code>
     * @param value The bytes for country to set.
     * @return This builder for chaining.
     */
    public Builder setCountryBytes(
        com.google.protobuf.ByteString value) {
      copyOnWrite();
      instance.setCountryBytes(value);
      return this;
    }

    /**
     * <code>string device_id = 4;</code>
     * @return The deviceId.
     */
    @java.lang.Override
    public java.lang.String getDeviceId() {
      return instance.getDeviceId();
    }
    /**
     * <code>string device_id = 4;</code>
     * @return The bytes for deviceId.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString
        getDeviceIdBytes() {
      return instance.getDeviceIdBytes();
    }
    /**
     * <code>string device_id = 4;</code>
     * @param value The deviceId to set.
     * @return This builder for chaining.
     */
    public Builder setDeviceId(
        java.lang.String value) {
      copyOnWrite();
      instance.setDeviceId(value);
      return this;
    }
    /**
     * <code>string device_id = 4;</code>
     * @return This builder for chaining.
     */
    public Builder clearDeviceId() {
      copyOnWrite();
      instance.clearDeviceId();
      return this;
    }
    /**
     * <code>string device_id = 4;</code>
     * @param value The bytes for deviceId to set.
     * @return This builder for chaining.
     */
    public Builder setDeviceIdBytes(
        com.google.protobuf.ByteString value) {
      copyOnWrite();
      instance.setDeviceIdBytes(value);
      return this;
    }

    /**
     * <code>repeated .api.Record records = 5;</code>
     */
    @java.lang.Override
    public java.util.List<com.pocketcasts.service.api.Record> getRecordsList() {
      return java.util.Collections.unmodifiableList(
          instance.getRecordsList());
    }
    /**
     * <code>repeated .api.Record records = 5;</code>
     */
    @java.lang.Override
    public int getRecordsCount() {
      return instance.getRecordsCount();
    }/**
     * <code>repeated .api.Record records = 5;</code>
     */
    @java.lang.Override
    public com.pocketcasts.service.api.Record getRecords(int index) {
      return instance.getRecords(index);
    }
    /**
     * <code>repeated .api.Record records = 5;</code>
     */
    public Builder setRecords(
        int index, com.pocketcasts.service.api.Record value) {
      copyOnWrite();
      instance.setRecords(index, value);
      return this;
    }
    /**
     * <code>repeated .api.Record records = 5;</code>
     */
    public Builder setRecords(
        int index, com.pocketcasts.service.api.Record.Builder builderForValue) {
      copyOnWrite();
      instance.setRecords(index,
          builderForValue.build());
      return this;
    }
    /**
     * <code>repeated .api.Record records = 5;</code>
     */
    public Builder addRecords(com.pocketcasts.service.api.Record value) {
      copyOnWrite();
      instance.addRecords(value);
      return this;
    }
    /**
     * <code>repeated .api.Record records = 5;</code>
     */
    public Builder addRecords(
        int index, com.pocketcasts.service.api.Record value) {
      copyOnWrite();
      instance.addRecords(index, value);
      return this;
    }
    /**
     * <code>repeated .api.Record records = 5;</code>
     */
    public Builder addRecords(
        com.pocketcasts.service.api.Record.Builder builderForValue) {
      copyOnWrite();
      instance.addRecords(builderForValue.build());
      return this;
    }
    /**
     * <code>repeated .api.Record records = 5;</code>
     */
    public Builder addRecords(
        int index, com.pocketcasts.service.api.Record.Builder builderForValue) {
      copyOnWrite();
      instance.addRecords(index,
          builderForValue.build());
      return this;
    }
    /**
     * <code>repeated .api.Record records = 5;</code>
     */
    public Builder addAllRecords(
        java.lang.Iterable<? extends com.pocketcasts.service.api.Record> values) {
      copyOnWrite();
      instance.addAllRecords(values);
      return this;
    }
    /**
     * <code>repeated .api.Record records = 5;</code>
     */
    public Builder clearRecords() {
      copyOnWrite();
      instance.clearRecords();
      return this;
    }
    /**
     * <code>repeated .api.Record records = 5;</code>
     */
    public Builder removeRecords(int index) {
      copyOnWrite();
      instance.removeRecords(index);
      return this;
    }

    // @@protoc_insertion_point(builder_scope:api.SyncUpdateRequest)
  }
  @java.lang.Override
  @java.lang.SuppressWarnings({"unchecked", "fallthrough"})
  protected final java.lang.Object dynamicMethod(
      com.google.protobuf.GeneratedMessageLite.MethodToInvoke method,
      java.lang.Object arg0, java.lang.Object arg1) {
    switch (method) {
      case NEW_MUTABLE_INSTANCE: {
        return new com.pocketcasts.service.api.SyncUpdateRequest();
      }
      case NEW_BUILDER: {
        return new Builder();
      }
      case BUILD_MESSAGE_INFO: {
          java.lang.Object[] objects = new java.lang.Object[] {
            "deviceUtcTimeMs_",
            "lastModified_",
            "country_",
            "deviceId_",
            "records_",
            com.pocketcasts.service.api.Record.class,
          };
          java.lang.String info =
              "\u0000\u0005\u0000\u0000\u0001\u0005\u0005\u0000\u0001\u0000\u0001\u0002\u0002\u0002" +
              "\u0003\u0208\u0004\u0208\u0005\u001b";
          return newMessageInfo(DEFAULT_INSTANCE, info, objects);
      }
      // fall through
      case GET_DEFAULT_INSTANCE: {
        return DEFAULT_INSTANCE;
      }
      case GET_PARSER: {
        com.google.protobuf.Parser<com.pocketcasts.service.api.SyncUpdateRequest> parser = PARSER;
        if (parser == null) {
          synchronized (com.pocketcasts.service.api.SyncUpdateRequest.class) {
            parser = PARSER;
            if (parser == null) {
              parser =
                  new DefaultInstanceBasedParser<com.pocketcasts.service.api.SyncUpdateRequest>(
                      DEFAULT_INSTANCE);
              PARSER = parser;
            }
          }
        }
        return parser;
    }
    case GET_MEMOIZED_IS_INITIALIZED: {
      return (byte) 1;
    }
    case SET_MEMOIZED_IS_INITIALIZED: {
      return null;
    }
    }
    throw new UnsupportedOperationException();
  }


  // @@protoc_insertion_point(class_scope:api.SyncUpdateRequest)
  private static final com.pocketcasts.service.api.SyncUpdateRequest DEFAULT_INSTANCE;
  static {
    SyncUpdateRequest defaultInstance = new SyncUpdateRequest();
    // New instances are implicitly immutable so no need to make
    // immutable.
    DEFAULT_INSTANCE = defaultInstance;
    com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(
      SyncUpdateRequest.class, defaultInstance);
  }

  public static com.pocketcasts.service.api.SyncUpdateRequest getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static volatile com.google.protobuf.Parser<SyncUpdateRequest> PARSER;

  public static com.google.protobuf.Parser<SyncUpdateRequest> parser() {
    return DEFAULT_INSTANCE.getParserForType();
  }
}

