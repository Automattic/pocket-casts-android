// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: api.proto

package com.pocketcasts.service.api;

/**
 * Protobuf type {@code api.YearHistoryRequest}
 */
public  final class YearHistoryRequest extends
    com.google.protobuf.GeneratedMessageLite<
        YearHistoryRequest, YearHistoryRequest.Builder> implements
    // @@protoc_insertion_point(message_implements:api.YearHistoryRequest)
    YearHistoryRequestOrBuilder {
  private YearHistoryRequest() {
    version_ = "";
  }
  public static final int VERSION_FIELD_NUMBER = 2;
  private java.lang.String version_;
  /**
   * <code>string version = 2;</code>
   * @return The version.
   */
  @java.lang.Override
  public java.lang.String getVersion() {
    return version_;
  }
  /**
   * <code>string version = 2;</code>
   * @return The bytes for version.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getVersionBytes() {
    return com.google.protobuf.ByteString.copyFromUtf8(version_);
  }
  /**
   * <code>string version = 2;</code>
   * @param value The version to set.
   */
  private void setVersion(
      java.lang.String value) {
    java.lang.Class<?> valueClass = value.getClass();
  
    version_ = value;
  }
  /**
   * <code>string version = 2;</code>
   */
  private void clearVersion() {

    version_ = getDefaultInstance().getVersion();
  }
  /**
   * <code>string version = 2;</code>
   * @param value The bytes for version to set.
   */
  private void setVersionBytes(
      com.google.protobuf.ByteString value) {
    checkByteStringIsUtf8(value);
    version_ = value.toStringUtf8();

  }

  public static final int COUNT_FIELD_NUMBER = 3;
  private boolean count_;
  /**
   * <code>bool count = 3;</code>
   * @return The count.
   */
  @java.lang.Override
  public boolean getCount() {
    return count_;
  }
  /**
   * <code>bool count = 3;</code>
   * @param value The count to set.
   */
  private void setCount(boolean value) {
    
    count_ = value;
  }
  /**
   * <code>bool count = 3;</code>
   */
  private void clearCount() {

    count_ = false;
  }

  public static final int YEAR_FIELD_NUMBER = 4;
  private int year_;
  /**
   * <code>int32 year = 4;</code>
   * @return The year.
   */
  @java.lang.Override
  public int getYear() {
    return year_;
  }
  /**
   * <code>int32 year = 4;</code>
   * @param value The year to set.
   */
  private void setYear(int value) {
    
    year_ = value;
  }
  /**
   * <code>int32 year = 4;</code>
   */
  private void clearYear() {

    year_ = 0;
  }

  public static com.pocketcasts.service.api.YearHistoryRequest parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.pocketcasts.service.api.YearHistoryRequest parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.pocketcasts.service.api.YearHistoryRequest parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.pocketcasts.service.api.YearHistoryRequest parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.pocketcasts.service.api.YearHistoryRequest parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.pocketcasts.service.api.YearHistoryRequest parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.pocketcasts.service.api.YearHistoryRequest parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input);
  }
  public static com.pocketcasts.service.api.YearHistoryRequest parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input, extensionRegistry);
  }

  public static com.pocketcasts.service.api.YearHistoryRequest parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return parseDelimitedFrom(DEFAULT_INSTANCE, input);
  }

  public static com.pocketcasts.service.api.YearHistoryRequest parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return parseDelimitedFrom(DEFAULT_INSTANCE, input, extensionRegistry);
  }
  public static com.pocketcasts.service.api.YearHistoryRequest parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input);
  }
  public static com.pocketcasts.service.api.YearHistoryRequest parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input, extensionRegistry);
  }

  public static Builder newBuilder() {
    return (Builder) DEFAULT_INSTANCE.createBuilder();
  }
  public static Builder newBuilder(com.pocketcasts.service.api.YearHistoryRequest prototype) {
    return (Builder) DEFAULT_INSTANCE.createBuilder(prototype);
  }

  /**
   * Protobuf type {@code api.YearHistoryRequest}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageLite.Builder<
        com.pocketcasts.service.api.YearHistoryRequest, Builder> implements
      // @@protoc_insertion_point(builder_implements:api.YearHistoryRequest)
      com.pocketcasts.service.api.YearHistoryRequestOrBuilder {
    // Construct using com.pocketcasts.service.api.YearHistoryRequest.newBuilder()
    private Builder() {
      super(DEFAULT_INSTANCE);
    }


    /**
     * <code>string version = 2;</code>
     * @return The version.
     */
    @java.lang.Override
    public java.lang.String getVersion() {
      return instance.getVersion();
    }
    /**
     * <code>string version = 2;</code>
     * @return The bytes for version.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString
        getVersionBytes() {
      return instance.getVersionBytes();
    }
    /**
     * <code>string version = 2;</code>
     * @param value The version to set.
     * @return This builder for chaining.
     */
    public Builder setVersion(
        java.lang.String value) {
      copyOnWrite();
      instance.setVersion(value);
      return this;
    }
    /**
     * <code>string version = 2;</code>
     * @return This builder for chaining.
     */
    public Builder clearVersion() {
      copyOnWrite();
      instance.clearVersion();
      return this;
    }
    /**
     * <code>string version = 2;</code>
     * @param value The bytes for version to set.
     * @return This builder for chaining.
     */
    public Builder setVersionBytes(
        com.google.protobuf.ByteString value) {
      copyOnWrite();
      instance.setVersionBytes(value);
      return this;
    }

    /**
     * <code>bool count = 3;</code>
     * @return The count.
     */
    @java.lang.Override
    public boolean getCount() {
      return instance.getCount();
    }
    /**
     * <code>bool count = 3;</code>
     * @param value The count to set.
     * @return This builder for chaining.
     */
    public Builder setCount(boolean value) {
      copyOnWrite();
      instance.setCount(value);
      return this;
    }
    /**
     * <code>bool count = 3;</code>
     * @return This builder for chaining.
     */
    public Builder clearCount() {
      copyOnWrite();
      instance.clearCount();
      return this;
    }

    /**
     * <code>int32 year = 4;</code>
     * @return The year.
     */
    @java.lang.Override
    public int getYear() {
      return instance.getYear();
    }
    /**
     * <code>int32 year = 4;</code>
     * @param value The year to set.
     * @return This builder for chaining.
     */
    public Builder setYear(int value) {
      copyOnWrite();
      instance.setYear(value);
      return this;
    }
    /**
     * <code>int32 year = 4;</code>
     * @return This builder for chaining.
     */
    public Builder clearYear() {
      copyOnWrite();
      instance.clearYear();
      return this;
    }

    // @@protoc_insertion_point(builder_scope:api.YearHistoryRequest)
  }
  @java.lang.Override
  @java.lang.SuppressWarnings({"unchecked", "fallthrough"})
  protected final java.lang.Object dynamicMethod(
      com.google.protobuf.GeneratedMessageLite.MethodToInvoke method,
      java.lang.Object arg0, java.lang.Object arg1) {
    switch (method) {
      case NEW_MUTABLE_INSTANCE: {
        return new com.pocketcasts.service.api.YearHistoryRequest();
      }
      case NEW_BUILDER: {
        return new Builder();
      }
      case BUILD_MESSAGE_INFO: {
          java.lang.Object[] objects = new java.lang.Object[] {
            "version_",
            "count_",
            "year_",
          };
          java.lang.String info =
              "\u0000\u0003\u0000\u0000\u0002\u0004\u0003\u0000\u0000\u0000\u0002\u0208\u0003\u0007" +
              "\u0004\u0004";
          return newMessageInfo(DEFAULT_INSTANCE, info, objects);
      }
      // fall through
      case GET_DEFAULT_INSTANCE: {
        return DEFAULT_INSTANCE;
      }
      case GET_PARSER: {
        com.google.protobuf.Parser<com.pocketcasts.service.api.YearHistoryRequest> parser = PARSER;
        if (parser == null) {
          synchronized (com.pocketcasts.service.api.YearHistoryRequest.class) {
            parser = PARSER;
            if (parser == null) {
              parser =
                  new DefaultInstanceBasedParser<com.pocketcasts.service.api.YearHistoryRequest>(
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


  // @@protoc_insertion_point(class_scope:api.YearHistoryRequest)
  private static final com.pocketcasts.service.api.YearHistoryRequest DEFAULT_INSTANCE;
  static {
    YearHistoryRequest defaultInstance = new YearHistoryRequest();
    // New instances are implicitly immutable so no need to make
    // immutable.
    DEFAULT_INSTANCE = defaultInstance;
    com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(
      YearHistoryRequest.class, defaultInstance);
  }

  public static com.pocketcasts.service.api.YearHistoryRequest getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static volatile com.google.protobuf.Parser<YearHistoryRequest> PARSER;

  public static com.google.protobuf.Parser<YearHistoryRequest> parser() {
    return DEFAULT_INSTANCE.getParserForType();
  }
}

