// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: files.proto

package com.pocketcasts.files.data;

/**
 * Protobuf type {@code files.FileUploadResponse}
 */
public  final class FileUploadResponse extends
    com.google.protobuf.GeneratedMessageLite<
        FileUploadResponse, FileUploadResponse.Builder> implements
    // @@protoc_insertion_point(message_implements:files.FileUploadResponse)
    FileUploadResponseOrBuilder {
  private FileUploadResponse() {
    uuid_ = "";
    url_ = "";
  }
  public static final int UUID_FIELD_NUMBER = 1;
  private java.lang.String uuid_;
  /**
   * <code>string uuid = 1;</code>
   * @return The uuid.
   */
  @java.lang.Override
  public java.lang.String getUuid() {
    return uuid_;
  }
  /**
   * <code>string uuid = 1;</code>
   * @return The bytes for uuid.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getUuidBytes() {
    return com.google.protobuf.ByteString.copyFromUtf8(uuid_);
  }
  /**
   * <code>string uuid = 1;</code>
   * @param value The uuid to set.
   */
  private void setUuid(
      java.lang.String value) {
    java.lang.Class<?> valueClass = value.getClass();
  
    uuid_ = value;
  }
  /**
   * <code>string uuid = 1;</code>
   */
  private void clearUuid() {

    uuid_ = getDefaultInstance().getUuid();
  }
  /**
   * <code>string uuid = 1;</code>
   * @param value The bytes for uuid to set.
   */
  private void setUuidBytes(
      com.google.protobuf.ByteString value) {
    checkByteStringIsUtf8(value);
    uuid_ = value.toStringUtf8();

  }

  public static final int URL_FIELD_NUMBER = 2;
  private java.lang.String url_;
  /**
   * <code>string url = 2;</code>
   * @return The url.
   */
  @java.lang.Override
  public java.lang.String getUrl() {
    return url_;
  }
  /**
   * <code>string url = 2;</code>
   * @return The bytes for url.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getUrlBytes() {
    return com.google.protobuf.ByteString.copyFromUtf8(url_);
  }
  /**
   * <code>string url = 2;</code>
   * @param value The url to set.
   */
  private void setUrl(
      java.lang.String value) {
    java.lang.Class<?> valueClass = value.getClass();
  
    url_ = value;
  }
  /**
   * <code>string url = 2;</code>
   */
  private void clearUrl() {

    url_ = getDefaultInstance().getUrl();
  }
  /**
   * <code>string url = 2;</code>
   * @param value The bytes for url to set.
   */
  private void setUrlBytes(
      com.google.protobuf.ByteString value) {
    checkByteStringIsUtf8(value);
    url_ = value.toStringUtf8();

  }

  public static com.pocketcasts.files.data.FileUploadResponse parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.pocketcasts.files.data.FileUploadResponse parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.pocketcasts.files.data.FileUploadResponse parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.pocketcasts.files.data.FileUploadResponse parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.pocketcasts.files.data.FileUploadResponse parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.pocketcasts.files.data.FileUploadResponse parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.pocketcasts.files.data.FileUploadResponse parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input);
  }
  public static com.pocketcasts.files.data.FileUploadResponse parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input, extensionRegistry);
  }

  public static com.pocketcasts.files.data.FileUploadResponse parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return parseDelimitedFrom(DEFAULT_INSTANCE, input);
  }

  public static com.pocketcasts.files.data.FileUploadResponse parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return parseDelimitedFrom(DEFAULT_INSTANCE, input, extensionRegistry);
  }
  public static com.pocketcasts.files.data.FileUploadResponse parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input);
  }
  public static com.pocketcasts.files.data.FileUploadResponse parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input, extensionRegistry);
  }

  public static Builder newBuilder() {
    return (Builder) DEFAULT_INSTANCE.createBuilder();
  }
  public static Builder newBuilder(com.pocketcasts.files.data.FileUploadResponse prototype) {
    return (Builder) DEFAULT_INSTANCE.createBuilder(prototype);
  }

  /**
   * Protobuf type {@code files.FileUploadResponse}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageLite.Builder<
        com.pocketcasts.files.data.FileUploadResponse, Builder> implements
      // @@protoc_insertion_point(builder_implements:files.FileUploadResponse)
      com.pocketcasts.files.data.FileUploadResponseOrBuilder {
    // Construct using com.pocketcasts.files.data.FileUploadResponse.newBuilder()
    private Builder() {
      super(DEFAULT_INSTANCE);
    }


    /**
     * <code>string uuid = 1;</code>
     * @return The uuid.
     */
    @java.lang.Override
    public java.lang.String getUuid() {
      return instance.getUuid();
    }
    /**
     * <code>string uuid = 1;</code>
     * @return The bytes for uuid.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString
        getUuidBytes() {
      return instance.getUuidBytes();
    }
    /**
     * <code>string uuid = 1;</code>
     * @param value The uuid to set.
     * @return This builder for chaining.
     */
    public Builder setUuid(
        java.lang.String value) {
      copyOnWrite();
      instance.setUuid(value);
      return this;
    }
    /**
     * <code>string uuid = 1;</code>
     * @return This builder for chaining.
     */
    public Builder clearUuid() {
      copyOnWrite();
      instance.clearUuid();
      return this;
    }
    /**
     * <code>string uuid = 1;</code>
     * @param value The bytes for uuid to set.
     * @return This builder for chaining.
     */
    public Builder setUuidBytes(
        com.google.protobuf.ByteString value) {
      copyOnWrite();
      instance.setUuidBytes(value);
      return this;
    }

    /**
     * <code>string url = 2;</code>
     * @return The url.
     */
    @java.lang.Override
    public java.lang.String getUrl() {
      return instance.getUrl();
    }
    /**
     * <code>string url = 2;</code>
     * @return The bytes for url.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString
        getUrlBytes() {
      return instance.getUrlBytes();
    }
    /**
     * <code>string url = 2;</code>
     * @param value The url to set.
     * @return This builder for chaining.
     */
    public Builder setUrl(
        java.lang.String value) {
      copyOnWrite();
      instance.setUrl(value);
      return this;
    }
    /**
     * <code>string url = 2;</code>
     * @return This builder for chaining.
     */
    public Builder clearUrl() {
      copyOnWrite();
      instance.clearUrl();
      return this;
    }
    /**
     * <code>string url = 2;</code>
     * @param value The bytes for url to set.
     * @return This builder for chaining.
     */
    public Builder setUrlBytes(
        com.google.protobuf.ByteString value) {
      copyOnWrite();
      instance.setUrlBytes(value);
      return this;
    }

    // @@protoc_insertion_point(builder_scope:files.FileUploadResponse)
  }
  @java.lang.Override
  @java.lang.SuppressWarnings({"unchecked", "fallthrough"})
  protected final java.lang.Object dynamicMethod(
      com.google.protobuf.GeneratedMessageLite.MethodToInvoke method,
      java.lang.Object arg0, java.lang.Object arg1) {
    switch (method) {
      case NEW_MUTABLE_INSTANCE: {
        return new com.pocketcasts.files.data.FileUploadResponse();
      }
      case NEW_BUILDER: {
        return new Builder();
      }
      case BUILD_MESSAGE_INFO: {
          java.lang.Object[] objects = new java.lang.Object[] {
            "uuid_",
            "url_",
          };
          java.lang.String info =
              "\u0000\u0002\u0000\u0000\u0001\u0002\u0002\u0000\u0000\u0000\u0001\u0208\u0002\u0208" +
              "";
          return newMessageInfo(DEFAULT_INSTANCE, info, objects);
      }
      // fall through
      case GET_DEFAULT_INSTANCE: {
        return DEFAULT_INSTANCE;
      }
      case GET_PARSER: {
        com.google.protobuf.Parser<com.pocketcasts.files.data.FileUploadResponse> parser = PARSER;
        if (parser == null) {
          synchronized (com.pocketcasts.files.data.FileUploadResponse.class) {
            parser = PARSER;
            if (parser == null) {
              parser =
                  new DefaultInstanceBasedParser<com.pocketcasts.files.data.FileUploadResponse>(
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


  // @@protoc_insertion_point(class_scope:files.FileUploadResponse)
  private static final com.pocketcasts.files.data.FileUploadResponse DEFAULT_INSTANCE;
  static {
    FileUploadResponse defaultInstance = new FileUploadResponse();
    // New instances are implicitly immutable so no need to make
    // immutable.
    DEFAULT_INSTANCE = defaultInstance;
    com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(
      FileUploadResponse.class, defaultInstance);
  }

  public static com.pocketcasts.files.data.FileUploadResponse getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static volatile com.google.protobuf.Parser<FileUploadResponse> PARSER;

  public static com.google.protobuf.Parser<FileUploadResponse> parser() {
    return DEFAULT_INSTANCE.getParserForType();
  }
}

