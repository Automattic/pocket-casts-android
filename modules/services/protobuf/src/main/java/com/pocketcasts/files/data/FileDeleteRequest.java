// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: files.proto

package com.pocketcasts.files.data;

/**
 * Protobuf type {@code files.FileDeleteRequest}
 */
public  final class FileDeleteRequest extends
    com.google.protobuf.GeneratedMessageLite<
        FileDeleteRequest, FileDeleteRequest.Builder> implements
    // @@protoc_insertion_point(message_implements:files.FileDeleteRequest)
    FileDeleteRequestOrBuilder {
  private FileDeleteRequest() {
    uuid_ = "";
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

  public static com.pocketcasts.files.data.FileDeleteRequest parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.pocketcasts.files.data.FileDeleteRequest parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.pocketcasts.files.data.FileDeleteRequest parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.pocketcasts.files.data.FileDeleteRequest parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.pocketcasts.files.data.FileDeleteRequest parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.pocketcasts.files.data.FileDeleteRequest parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.pocketcasts.files.data.FileDeleteRequest parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input);
  }
  public static com.pocketcasts.files.data.FileDeleteRequest parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input, extensionRegistry);
  }

  public static com.pocketcasts.files.data.FileDeleteRequest parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return parseDelimitedFrom(DEFAULT_INSTANCE, input);
  }

  public static com.pocketcasts.files.data.FileDeleteRequest parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return parseDelimitedFrom(DEFAULT_INSTANCE, input, extensionRegistry);
  }
  public static com.pocketcasts.files.data.FileDeleteRequest parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input);
  }
  public static com.pocketcasts.files.data.FileDeleteRequest parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input, extensionRegistry);
  }

  public static Builder newBuilder() {
    return (Builder) DEFAULT_INSTANCE.createBuilder();
  }
  public static Builder newBuilder(com.pocketcasts.files.data.FileDeleteRequest prototype) {
    return (Builder) DEFAULT_INSTANCE.createBuilder(prototype);
  }

  /**
   * Protobuf type {@code files.FileDeleteRequest}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageLite.Builder<
        com.pocketcasts.files.data.FileDeleteRequest, Builder> implements
      // @@protoc_insertion_point(builder_implements:files.FileDeleteRequest)
      com.pocketcasts.files.data.FileDeleteRequestOrBuilder {
    // Construct using com.pocketcasts.files.data.FileDeleteRequest.newBuilder()
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

    // @@protoc_insertion_point(builder_scope:files.FileDeleteRequest)
  }
  @java.lang.Override
  @java.lang.SuppressWarnings({"unchecked", "fallthrough"})
  protected final java.lang.Object dynamicMethod(
      com.google.protobuf.GeneratedMessageLite.MethodToInvoke method,
      java.lang.Object arg0, java.lang.Object arg1) {
    switch (method) {
      case NEW_MUTABLE_INSTANCE: {
        return new com.pocketcasts.files.data.FileDeleteRequest();
      }
      case NEW_BUILDER: {
        return new Builder();
      }
      case BUILD_MESSAGE_INFO: {
          java.lang.Object[] objects = new java.lang.Object[] {
            "uuid_",
          };
          java.lang.String info =
              "\u0000\u0001\u0000\u0000\u0001\u0001\u0001\u0000\u0000\u0000\u0001\u0208";
          return newMessageInfo(DEFAULT_INSTANCE, info, objects);
      }
      // fall through
      case GET_DEFAULT_INSTANCE: {
        return DEFAULT_INSTANCE;
      }
      case GET_PARSER: {
        com.google.protobuf.Parser<com.pocketcasts.files.data.FileDeleteRequest> parser = PARSER;
        if (parser == null) {
          synchronized (com.pocketcasts.files.data.FileDeleteRequest.class) {
            parser = PARSER;
            if (parser == null) {
              parser =
                  new DefaultInstanceBasedParser<com.pocketcasts.files.data.FileDeleteRequest>(
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


  // @@protoc_insertion_point(class_scope:files.FileDeleteRequest)
  private static final com.pocketcasts.files.data.FileDeleteRequest DEFAULT_INSTANCE;
  static {
    FileDeleteRequest defaultInstance = new FileDeleteRequest();
    // New instances are implicitly immutable so no need to make
    // immutable.
    DEFAULT_INSTANCE = defaultInstance;
    com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(
      FileDeleteRequest.class, defaultInstance);
  }

  public static com.pocketcasts.files.data.FileDeleteRequest getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static volatile com.google.protobuf.Parser<FileDeleteRequest> PARSER;

  public static com.google.protobuf.Parser<FileDeleteRequest> parser() {
    return DEFAULT_INSTANCE.getParserForType();
  }
}

