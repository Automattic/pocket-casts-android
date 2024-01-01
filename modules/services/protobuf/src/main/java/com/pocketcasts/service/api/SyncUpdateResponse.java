// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: api.proto

// Protobuf Java Version: 3.25.1
package com.pocketcasts.service.api;

/**
 * Protobuf type {@code api.SyncUpdateResponse}
 */
public  final class SyncUpdateResponse extends
    com.google.protobuf.GeneratedMessageLite<
        SyncUpdateResponse, SyncUpdateResponse.Builder> implements
    // @@protoc_insertion_point(message_implements:api.SyncUpdateResponse)
    SyncUpdateResponseOrBuilder {
  private SyncUpdateResponse() {
    records_ = emptyProtobufList();
  }
  public static final int LAST_MODIFIED_FIELD_NUMBER = 1;
  private long lastModified_;
  /**
   * <code>int64 last_modified = 1;</code>
   * @return The lastModified.
   */
  @java.lang.Override
  public long getLastModified() {
    return lastModified_;
  }
  /**
   * <code>int64 last_modified = 1;</code>
   * @param value The lastModified to set.
   */
  private void setLastModified(long value) {
    
    lastModified_ = value;
  }
  /**
   * <code>int64 last_modified = 1;</code>
   */
  private void clearLastModified() {

    lastModified_ = 0L;
  }

  public static final int RECORDS_FIELD_NUMBER = 2;
  private com.google.protobuf.Internal.ProtobufList<com.pocketcasts.service.api.Record> records_;
  /**
   * <code>repeated .api.Record records = 2;</code>
   */
  @java.lang.Override
  public java.util.List<com.pocketcasts.service.api.Record> getRecordsList() {
    return records_;
  }
  /**
   * <code>repeated .api.Record records = 2;</code>
   */
  public java.util.List<? extends com.pocketcasts.service.api.RecordOrBuilder> 
      getRecordsOrBuilderList() {
    return records_;
  }
  /**
   * <code>repeated .api.Record records = 2;</code>
   */
  @java.lang.Override
  public int getRecordsCount() {
    return records_.size();
  }
  /**
   * <code>repeated .api.Record records = 2;</code>
   */
  @java.lang.Override
  public com.pocketcasts.service.api.Record getRecords(int index) {
    return records_.get(index);
  }
  /**
   * <code>repeated .api.Record records = 2;</code>
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
   * <code>repeated .api.Record records = 2;</code>
   */
  private void setRecords(
      int index, com.pocketcasts.service.api.Record value) {
    value.getClass();
  ensureRecordsIsMutable();
    records_.set(index, value);
  }
  /**
   * <code>repeated .api.Record records = 2;</code>
   */
  private void addRecords(com.pocketcasts.service.api.Record value) {
    value.getClass();
  ensureRecordsIsMutable();
    records_.add(value);
  }
  /**
   * <code>repeated .api.Record records = 2;</code>
   */
  private void addRecords(
      int index, com.pocketcasts.service.api.Record value) {
    value.getClass();
  ensureRecordsIsMutable();
    records_.add(index, value);
  }
  /**
   * <code>repeated .api.Record records = 2;</code>
   */
  private void addAllRecords(
      java.lang.Iterable<? extends com.pocketcasts.service.api.Record> values) {
    ensureRecordsIsMutable();
    com.google.protobuf.AbstractMessageLite.addAll(
        values, records_);
  }
  /**
   * <code>repeated .api.Record records = 2;</code>
   */
  private void clearRecords() {
    records_ = emptyProtobufList();
  }
  /**
   * <code>repeated .api.Record records = 2;</code>
   */
  private void removeRecords(int index) {
    ensureRecordsIsMutable();
    records_.remove(index);
  }

  public static com.pocketcasts.service.api.SyncUpdateResponse parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.pocketcasts.service.api.SyncUpdateResponse parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.pocketcasts.service.api.SyncUpdateResponse parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.pocketcasts.service.api.SyncUpdateResponse parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.pocketcasts.service.api.SyncUpdateResponse parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.pocketcasts.service.api.SyncUpdateResponse parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.pocketcasts.service.api.SyncUpdateResponse parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input);
  }
  public static com.pocketcasts.service.api.SyncUpdateResponse parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input, extensionRegistry);
  }

  public static com.pocketcasts.service.api.SyncUpdateResponse parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return parseDelimitedFrom(DEFAULT_INSTANCE, input);
  }

  public static com.pocketcasts.service.api.SyncUpdateResponse parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return parseDelimitedFrom(DEFAULT_INSTANCE, input, extensionRegistry);
  }
  public static com.pocketcasts.service.api.SyncUpdateResponse parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input);
  }
  public static com.pocketcasts.service.api.SyncUpdateResponse parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input, extensionRegistry);
  }

  public static Builder newBuilder() {
    return (Builder) DEFAULT_INSTANCE.createBuilder();
  }
  public static Builder newBuilder(com.pocketcasts.service.api.SyncUpdateResponse prototype) {
    return DEFAULT_INSTANCE.createBuilder(prototype);
  }

  /**
   * Protobuf type {@code api.SyncUpdateResponse}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageLite.Builder<
        com.pocketcasts.service.api.SyncUpdateResponse, Builder> implements
      // @@protoc_insertion_point(builder_implements:api.SyncUpdateResponse)
      com.pocketcasts.service.api.SyncUpdateResponseOrBuilder {
    // Construct using com.pocketcasts.service.api.SyncUpdateResponse.newBuilder()
    private Builder() {
      super(DEFAULT_INSTANCE);
    }


    /**
     * <code>int64 last_modified = 1;</code>
     * @return The lastModified.
     */
    @java.lang.Override
    public long getLastModified() {
      return instance.getLastModified();
    }
    /**
     * <code>int64 last_modified = 1;</code>
     * @param value The lastModified to set.
     * @return This builder for chaining.
     */
    public Builder setLastModified(long value) {
      copyOnWrite();
      instance.setLastModified(value);
      return this;
    }
    /**
     * <code>int64 last_modified = 1;</code>
     * @return This builder for chaining.
     */
    public Builder clearLastModified() {
      copyOnWrite();
      instance.clearLastModified();
      return this;
    }

    /**
     * <code>repeated .api.Record records = 2;</code>
     */
    @java.lang.Override
    public java.util.List<com.pocketcasts.service.api.Record> getRecordsList() {
      return java.util.Collections.unmodifiableList(
          instance.getRecordsList());
    }
    /**
     * <code>repeated .api.Record records = 2;</code>
     */
    @java.lang.Override
    public int getRecordsCount() {
      return instance.getRecordsCount();
    }/**
     * <code>repeated .api.Record records = 2;</code>
     */
    @java.lang.Override
    public com.pocketcasts.service.api.Record getRecords(int index) {
      return instance.getRecords(index);
    }
    /**
     * <code>repeated .api.Record records = 2;</code>
     */
    public Builder setRecords(
        int index, com.pocketcasts.service.api.Record value) {
      copyOnWrite();
      instance.setRecords(index, value);
      return this;
    }
    /**
     * <code>repeated .api.Record records = 2;</code>
     */
    public Builder setRecords(
        int index, com.pocketcasts.service.api.Record.Builder builderForValue) {
      copyOnWrite();
      instance.setRecords(index,
          builderForValue.build());
      return this;
    }
    /**
     * <code>repeated .api.Record records = 2;</code>
     */
    public Builder addRecords(com.pocketcasts.service.api.Record value) {
      copyOnWrite();
      instance.addRecords(value);
      return this;
    }
    /**
     * <code>repeated .api.Record records = 2;</code>
     */
    public Builder addRecords(
        int index, com.pocketcasts.service.api.Record value) {
      copyOnWrite();
      instance.addRecords(index, value);
      return this;
    }
    /**
     * <code>repeated .api.Record records = 2;</code>
     */
    public Builder addRecords(
        com.pocketcasts.service.api.Record.Builder builderForValue) {
      copyOnWrite();
      instance.addRecords(builderForValue.build());
      return this;
    }
    /**
     * <code>repeated .api.Record records = 2;</code>
     */
    public Builder addRecords(
        int index, com.pocketcasts.service.api.Record.Builder builderForValue) {
      copyOnWrite();
      instance.addRecords(index,
          builderForValue.build());
      return this;
    }
    /**
     * <code>repeated .api.Record records = 2;</code>
     */
    public Builder addAllRecords(
        java.lang.Iterable<? extends com.pocketcasts.service.api.Record> values) {
      copyOnWrite();
      instance.addAllRecords(values);
      return this;
    }
    /**
     * <code>repeated .api.Record records = 2;</code>
     */
    public Builder clearRecords() {
      copyOnWrite();
      instance.clearRecords();
      return this;
    }
    /**
     * <code>repeated .api.Record records = 2;</code>
     */
    public Builder removeRecords(int index) {
      copyOnWrite();
      instance.removeRecords(index);
      return this;
    }

    // @@protoc_insertion_point(builder_scope:api.SyncUpdateResponse)
  }
  @java.lang.Override
  @java.lang.SuppressWarnings({"unchecked", "fallthrough"})
  protected final java.lang.Object dynamicMethod(
      com.google.protobuf.GeneratedMessageLite.MethodToInvoke method,
      java.lang.Object arg0, java.lang.Object arg1) {
    switch (method) {
      case NEW_MUTABLE_INSTANCE: {
        return new com.pocketcasts.service.api.SyncUpdateResponse();
      }
      case NEW_BUILDER: {
        return new Builder();
      }
      case BUILD_MESSAGE_INFO: {
          java.lang.Object[] objects = new java.lang.Object[] {
            "lastModified_",
            "records_",
            com.pocketcasts.service.api.Record.class,
          };
          java.lang.String info =
              "\u0000\u0002\u0000\u0000\u0001\u0002\u0002\u0000\u0001\u0000\u0001\u0002\u0002\u001b" +
              "";
          return newMessageInfo(DEFAULT_INSTANCE, info, objects);
      }
      // fall through
      case GET_DEFAULT_INSTANCE: {
        return DEFAULT_INSTANCE;
      }
      case GET_PARSER: {
        com.google.protobuf.Parser<com.pocketcasts.service.api.SyncUpdateResponse> parser = PARSER;
        if (parser == null) {
          synchronized (com.pocketcasts.service.api.SyncUpdateResponse.class) {
            parser = PARSER;
            if (parser == null) {
              parser =
                  new DefaultInstanceBasedParser<com.pocketcasts.service.api.SyncUpdateResponse>(
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


  // @@protoc_insertion_point(class_scope:api.SyncUpdateResponse)
  private static final com.pocketcasts.service.api.SyncUpdateResponse DEFAULT_INSTANCE;
  static {
    SyncUpdateResponse defaultInstance = new SyncUpdateResponse();
    // New instances are implicitly immutable so no need to make
    // immutable.
    DEFAULT_INSTANCE = defaultInstance;
    com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(
      SyncUpdateResponse.class, defaultInstance);
  }

  public static com.pocketcasts.service.api.SyncUpdateResponse getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static volatile com.google.protobuf.Parser<SyncUpdateResponse> PARSER;

  public static com.google.protobuf.Parser<SyncUpdateResponse> parser() {
    return DEFAULT_INSTANCE.getParserForType();
  }
}

