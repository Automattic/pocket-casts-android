// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: api.proto

package com.pocketcasts.service.api;

/**
 * Protobuf type {@code api.StringSetting}
 */
public  final class StringSetting extends
    com.google.protobuf.GeneratedMessageLite<
        StringSetting, StringSetting.Builder> implements
    // @@protoc_insertion_point(message_implements:api.StringSetting)
    StringSettingOrBuilder {
  private StringSetting() {
  }
  public static final int VALUE_FIELD_NUMBER = 1;
  private com.google.protobuf.StringValue value_;
  /**
   * <code>.google.protobuf.StringValue value = 1;</code>
   */
  @java.lang.Override
  public boolean hasValue() {
    return value_ != null;
  }
  /**
   * <code>.google.protobuf.StringValue value = 1;</code>
   */
  @java.lang.Override
  public com.google.protobuf.StringValue getValue() {
    return value_ == null ? com.google.protobuf.StringValue.getDefaultInstance() : value_;
  }
  /**
   * <code>.google.protobuf.StringValue value = 1;</code>
   */
  private void setValue(com.google.protobuf.StringValue value) {
    value.getClass();
  value_ = value;

    }
  /**
   * <code>.google.protobuf.StringValue value = 1;</code>
   */
  @java.lang.SuppressWarnings({"ReferenceEquality"})
  private void mergeValue(com.google.protobuf.StringValue value) {
    value.getClass();
  if (value_ != null &&
        value_ != com.google.protobuf.StringValue.getDefaultInstance()) {
      value_ =
        com.google.protobuf.StringValue.newBuilder(value_).mergeFrom(value).buildPartial();
    } else {
      value_ = value;
    }

  }
  /**
   * <code>.google.protobuf.StringValue value = 1;</code>
   */
  private void clearValue() {  value_ = null;

  }

  public static final int CHANGED_FIELD_NUMBER = 2;
  private com.google.protobuf.BoolValue changed_;
  /**
   * <code>.google.protobuf.BoolValue changed = 2;</code>
   */
  @java.lang.Override
  public boolean hasChanged() {
    return changed_ != null;
  }
  /**
   * <code>.google.protobuf.BoolValue changed = 2;</code>
   */
  @java.lang.Override
  public com.google.protobuf.BoolValue getChanged() {
    return changed_ == null ? com.google.protobuf.BoolValue.getDefaultInstance() : changed_;
  }
  /**
   * <code>.google.protobuf.BoolValue changed = 2;</code>
   */
  private void setChanged(com.google.protobuf.BoolValue value) {
    value.getClass();
  changed_ = value;

    }
  /**
   * <code>.google.protobuf.BoolValue changed = 2;</code>
   */
  @java.lang.SuppressWarnings({"ReferenceEquality"})
  private void mergeChanged(com.google.protobuf.BoolValue value) {
    value.getClass();
  if (changed_ != null &&
        changed_ != com.google.protobuf.BoolValue.getDefaultInstance()) {
      changed_ =
        com.google.protobuf.BoolValue.newBuilder(changed_).mergeFrom(value).buildPartial();
    } else {
      changed_ = value;
    }

  }
  /**
   * <code>.google.protobuf.BoolValue changed = 2;</code>
   */
  private void clearChanged() {  changed_ = null;

  }

  public static com.pocketcasts.service.api.StringSetting parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.pocketcasts.service.api.StringSetting parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.pocketcasts.service.api.StringSetting parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.pocketcasts.service.api.StringSetting parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.pocketcasts.service.api.StringSetting parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.pocketcasts.service.api.StringSetting parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.pocketcasts.service.api.StringSetting parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input);
  }
  public static com.pocketcasts.service.api.StringSetting parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input, extensionRegistry);
  }

  public static com.pocketcasts.service.api.StringSetting parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return parseDelimitedFrom(DEFAULT_INSTANCE, input);
  }

  public static com.pocketcasts.service.api.StringSetting parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return parseDelimitedFrom(DEFAULT_INSTANCE, input, extensionRegistry);
  }
  public static com.pocketcasts.service.api.StringSetting parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input);
  }
  public static com.pocketcasts.service.api.StringSetting parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input, extensionRegistry);
  }

  public static Builder newBuilder() {
    return (Builder) DEFAULT_INSTANCE.createBuilder();
  }
  public static Builder newBuilder(com.pocketcasts.service.api.StringSetting prototype) {
    return (Builder) DEFAULT_INSTANCE.createBuilder(prototype);
  }

  /**
   * Protobuf type {@code api.StringSetting}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageLite.Builder<
        com.pocketcasts.service.api.StringSetting, Builder> implements
      // @@protoc_insertion_point(builder_implements:api.StringSetting)
      com.pocketcasts.service.api.StringSettingOrBuilder {
    // Construct using com.pocketcasts.service.api.StringSetting.newBuilder()
    private Builder() {
      super(DEFAULT_INSTANCE);
    }


    /**
     * <code>.google.protobuf.StringValue value = 1;</code>
     */
    @java.lang.Override
    public boolean hasValue() {
      return instance.hasValue();
    }
    /**
     * <code>.google.protobuf.StringValue value = 1;</code>
     */
    @java.lang.Override
    public com.google.protobuf.StringValue getValue() {
      return instance.getValue();
    }
    /**
     * <code>.google.protobuf.StringValue value = 1;</code>
     */
    public Builder setValue(com.google.protobuf.StringValue value) {
      copyOnWrite();
      instance.setValue(value);
      return this;
      }
    /**
     * <code>.google.protobuf.StringValue value = 1;</code>
     */
    public Builder setValue(
        com.google.protobuf.StringValue.Builder builderForValue) {
      copyOnWrite();
      instance.setValue(builderForValue.build());
      return this;
    }
    /**
     * <code>.google.protobuf.StringValue value = 1;</code>
     */
    public Builder mergeValue(com.google.protobuf.StringValue value) {
      copyOnWrite();
      instance.mergeValue(value);
      return this;
    }
    /**
     * <code>.google.protobuf.StringValue value = 1;</code>
     */
    public Builder clearValue() {  copyOnWrite();
      instance.clearValue();
      return this;
    }

    /**
     * <code>.google.protobuf.BoolValue changed = 2;</code>
     */
    @java.lang.Override
    public boolean hasChanged() {
      return instance.hasChanged();
    }
    /**
     * <code>.google.protobuf.BoolValue changed = 2;</code>
     */
    @java.lang.Override
    public com.google.protobuf.BoolValue getChanged() {
      return instance.getChanged();
    }
    /**
     * <code>.google.protobuf.BoolValue changed = 2;</code>
     */
    public Builder setChanged(com.google.protobuf.BoolValue value) {
      copyOnWrite();
      instance.setChanged(value);
      return this;
      }
    /**
     * <code>.google.protobuf.BoolValue changed = 2;</code>
     */
    public Builder setChanged(
        com.google.protobuf.BoolValue.Builder builderForValue) {
      copyOnWrite();
      instance.setChanged(builderForValue.build());
      return this;
    }
    /**
     * <code>.google.protobuf.BoolValue changed = 2;</code>
     */
    public Builder mergeChanged(com.google.protobuf.BoolValue value) {
      copyOnWrite();
      instance.mergeChanged(value);
      return this;
    }
    /**
     * <code>.google.protobuf.BoolValue changed = 2;</code>
     */
    public Builder clearChanged() {  copyOnWrite();
      instance.clearChanged();
      return this;
    }

    // @@protoc_insertion_point(builder_scope:api.StringSetting)
  }
  @java.lang.Override
  @java.lang.SuppressWarnings({"unchecked", "fallthrough"})
  protected final java.lang.Object dynamicMethod(
      com.google.protobuf.GeneratedMessageLite.MethodToInvoke method,
      java.lang.Object arg0, java.lang.Object arg1) {
    switch (method) {
      case NEW_MUTABLE_INSTANCE: {
        return new com.pocketcasts.service.api.StringSetting();
      }
      case NEW_BUILDER: {
        return new Builder();
      }
      case BUILD_MESSAGE_INFO: {
          java.lang.Object[] objects = new java.lang.Object[] {
            "value_",
            "changed_",
          };
          java.lang.String info =
              "\u0000\u0002\u0000\u0000\u0001\u0002\u0002\u0000\u0000\u0000\u0001\t\u0002\t";
          return newMessageInfo(DEFAULT_INSTANCE, info, objects);
      }
      // fall through
      case GET_DEFAULT_INSTANCE: {
        return DEFAULT_INSTANCE;
      }
      case GET_PARSER: {
        com.google.protobuf.Parser<com.pocketcasts.service.api.StringSetting> parser = PARSER;
        if (parser == null) {
          synchronized (com.pocketcasts.service.api.StringSetting.class) {
            parser = PARSER;
            if (parser == null) {
              parser =
                  new DefaultInstanceBasedParser<com.pocketcasts.service.api.StringSetting>(
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


  // @@protoc_insertion_point(class_scope:api.StringSetting)
  private static final com.pocketcasts.service.api.StringSetting DEFAULT_INSTANCE;
  static {
    StringSetting defaultInstance = new StringSetting();
    // New instances are implicitly immutable so no need to make
    // immutable.
    DEFAULT_INSTANCE = defaultInstance;
    com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(
      StringSetting.class, defaultInstance);
  }

  public static com.pocketcasts.service.api.StringSetting getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static volatile com.google.protobuf.Parser<StringSetting> PARSER;

  public static com.google.protobuf.Parser<StringSetting> parser() {
    return DEFAULT_INSTANCE.getParserForType();
  }
}

