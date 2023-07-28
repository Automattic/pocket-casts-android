// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: api.proto

package com.pocketcasts.service.api;

/**
 * Protobuf type {@code api.LegacyStatsResponse}
 */
public  final class LegacyStatsResponse extends
    com.google.protobuf.GeneratedMessageLite<
        LegacyStatsResponse, LegacyStatsResponse.Builder> implements
    // @@protoc_insertion_point(message_implements:api.LegacyStatsResponse)
    LegacyStatsResponseOrBuilder {
  private LegacyStatsResponse() {
  }
  public static final int TIMES_STARTED_AT_FIELD_NUMBER = 1;
  private int timesStartedAt_;
  /**
   * <code>int32 times_started_at = 1;</code>
   * @return The timesStartedAt.
   */
  @java.lang.Override
  public int getTimesStartedAt() {
    return timesStartedAt_;
  }
  /**
   * <code>int32 times_started_at = 1;</code>
   * @param value The timesStartedAt to set.
   */
  private void setTimesStartedAt(int value) {
    
    timesStartedAt_ = value;
  }
  /**
   * <code>int32 times_started_at = 1;</code>
   */
  private void clearTimesStartedAt() {

    timesStartedAt_ = 0;
  }

  public static final int TIME_SILENCE_REMOVAL_FIELD_NUMBER = 2;
  private int timeSilenceRemoval_;
  /**
   * <code>int32 time_silence_removal = 2;</code>
   * @return The timeSilenceRemoval.
   */
  @java.lang.Override
  public int getTimeSilenceRemoval() {
    return timeSilenceRemoval_;
  }
  /**
   * <code>int32 time_silence_removal = 2;</code>
   * @param value The timeSilenceRemoval to set.
   */
  private void setTimeSilenceRemoval(int value) {
    
    timeSilenceRemoval_ = value;
  }
  /**
   * <code>int32 time_silence_removal = 2;</code>
   */
  private void clearTimeSilenceRemoval() {

    timeSilenceRemoval_ = 0;
  }

  public static final int TIME_VARIABLE_SPEED_FIELD_NUMBER = 3;
  private int timeVariableSpeed_;
  /**
   * <code>int32 time_variable_speed = 3;</code>
   * @return The timeVariableSpeed.
   */
  @java.lang.Override
  public int getTimeVariableSpeed() {
    return timeVariableSpeed_;
  }
  /**
   * <code>int32 time_variable_speed = 3;</code>
   * @param value The timeVariableSpeed to set.
   */
  private void setTimeVariableSpeed(int value) {
    
    timeVariableSpeed_ = value;
  }
  /**
   * <code>int32 time_variable_speed = 3;</code>
   */
  private void clearTimeVariableSpeed() {

    timeVariableSpeed_ = 0;
  }

  public static final int TIME_INTRO_SKIPPING_FIELD_NUMBER = 4;
  private int timeIntroSkipping_;
  /**
   * <code>int32 time_intro_skipping = 4;</code>
   * @return The timeIntroSkipping.
   */
  @java.lang.Override
  public int getTimeIntroSkipping() {
    return timeIntroSkipping_;
  }
  /**
   * <code>int32 time_intro_skipping = 4;</code>
   * @param value The timeIntroSkipping to set.
   */
  private void setTimeIntroSkipping(int value) {
    
    timeIntroSkipping_ = value;
  }
  /**
   * <code>int32 time_intro_skipping = 4;</code>
   */
  private void clearTimeIntroSkipping() {

    timeIntroSkipping_ = 0;
  }

  public static final int TIME_SKIPPING_FIELD_NUMBER = 5;
  private int timeSkipping_;
  /**
   * <code>int32 time_skipping = 5;</code>
   * @return The timeSkipping.
   */
  @java.lang.Override
  public int getTimeSkipping() {
    return timeSkipping_;
  }
  /**
   * <code>int32 time_skipping = 5;</code>
   * @param value The timeSkipping to set.
   */
  private void setTimeSkipping(int value) {
    
    timeSkipping_ = value;
  }
  /**
   * <code>int32 time_skipping = 5;</code>
   */
  private void clearTimeSkipping() {

    timeSkipping_ = 0;
  }

  public static final int TIME_LISTENED_FIELD_NUMBER = 6;
  private int timeListened_;
  /**
   * <code>int32 time_listened = 6;</code>
   * @return The timeListened.
   */
  @java.lang.Override
  public int getTimeListened() {
    return timeListened_;
  }
  /**
   * <code>int32 time_listened = 6;</code>
   * @param value The timeListened to set.
   */
  private void setTimeListened(int value) {
    
    timeListened_ = value;
  }
  /**
   * <code>int32 time_listened = 6;</code>
   */
  private void clearTimeListened() {

    timeListened_ = 0;
  }

  public static com.pocketcasts.service.api.LegacyStatsResponse parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.pocketcasts.service.api.LegacyStatsResponse parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.pocketcasts.service.api.LegacyStatsResponse parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.pocketcasts.service.api.LegacyStatsResponse parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.pocketcasts.service.api.LegacyStatsResponse parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.pocketcasts.service.api.LegacyStatsResponse parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.pocketcasts.service.api.LegacyStatsResponse parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input);
  }
  public static com.pocketcasts.service.api.LegacyStatsResponse parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input, extensionRegistry);
  }

  public static com.pocketcasts.service.api.LegacyStatsResponse parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return parseDelimitedFrom(DEFAULT_INSTANCE, input);
  }

  public static com.pocketcasts.service.api.LegacyStatsResponse parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return parseDelimitedFrom(DEFAULT_INSTANCE, input, extensionRegistry);
  }
  public static com.pocketcasts.service.api.LegacyStatsResponse parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input);
  }
  public static com.pocketcasts.service.api.LegacyStatsResponse parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input, extensionRegistry);
  }

  public static Builder newBuilder() {
    return (Builder) DEFAULT_INSTANCE.createBuilder();
  }
  public static Builder newBuilder(com.pocketcasts.service.api.LegacyStatsResponse prototype) {
    return (Builder) DEFAULT_INSTANCE.createBuilder(prototype);
  }

  /**
   * Protobuf type {@code api.LegacyStatsResponse}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageLite.Builder<
        com.pocketcasts.service.api.LegacyStatsResponse, Builder> implements
      // @@protoc_insertion_point(builder_implements:api.LegacyStatsResponse)
      com.pocketcasts.service.api.LegacyStatsResponseOrBuilder {
    // Construct using com.pocketcasts.service.api.LegacyStatsResponse.newBuilder()
    private Builder() {
      super(DEFAULT_INSTANCE);
    }


    /**
     * <code>int32 times_started_at = 1;</code>
     * @return The timesStartedAt.
     */
    @java.lang.Override
    public int getTimesStartedAt() {
      return instance.getTimesStartedAt();
    }
    /**
     * <code>int32 times_started_at = 1;</code>
     * @param value The timesStartedAt to set.
     * @return This builder for chaining.
     */
    public Builder setTimesStartedAt(int value) {
      copyOnWrite();
      instance.setTimesStartedAt(value);
      return this;
    }
    /**
     * <code>int32 times_started_at = 1;</code>
     * @return This builder for chaining.
     */
    public Builder clearTimesStartedAt() {
      copyOnWrite();
      instance.clearTimesStartedAt();
      return this;
    }

    /**
     * <code>int32 time_silence_removal = 2;</code>
     * @return The timeSilenceRemoval.
     */
    @java.lang.Override
    public int getTimeSilenceRemoval() {
      return instance.getTimeSilenceRemoval();
    }
    /**
     * <code>int32 time_silence_removal = 2;</code>
     * @param value The timeSilenceRemoval to set.
     * @return This builder for chaining.
     */
    public Builder setTimeSilenceRemoval(int value) {
      copyOnWrite();
      instance.setTimeSilenceRemoval(value);
      return this;
    }
    /**
     * <code>int32 time_silence_removal = 2;</code>
     * @return This builder for chaining.
     */
    public Builder clearTimeSilenceRemoval() {
      copyOnWrite();
      instance.clearTimeSilenceRemoval();
      return this;
    }

    /**
     * <code>int32 time_variable_speed = 3;</code>
     * @return The timeVariableSpeed.
     */
    @java.lang.Override
    public int getTimeVariableSpeed() {
      return instance.getTimeVariableSpeed();
    }
    /**
     * <code>int32 time_variable_speed = 3;</code>
     * @param value The timeVariableSpeed to set.
     * @return This builder for chaining.
     */
    public Builder setTimeVariableSpeed(int value) {
      copyOnWrite();
      instance.setTimeVariableSpeed(value);
      return this;
    }
    /**
     * <code>int32 time_variable_speed = 3;</code>
     * @return This builder for chaining.
     */
    public Builder clearTimeVariableSpeed() {
      copyOnWrite();
      instance.clearTimeVariableSpeed();
      return this;
    }

    /**
     * <code>int32 time_intro_skipping = 4;</code>
     * @return The timeIntroSkipping.
     */
    @java.lang.Override
    public int getTimeIntroSkipping() {
      return instance.getTimeIntroSkipping();
    }
    /**
     * <code>int32 time_intro_skipping = 4;</code>
     * @param value The timeIntroSkipping to set.
     * @return This builder for chaining.
     */
    public Builder setTimeIntroSkipping(int value) {
      copyOnWrite();
      instance.setTimeIntroSkipping(value);
      return this;
    }
    /**
     * <code>int32 time_intro_skipping = 4;</code>
     * @return This builder for chaining.
     */
    public Builder clearTimeIntroSkipping() {
      copyOnWrite();
      instance.clearTimeIntroSkipping();
      return this;
    }

    /**
     * <code>int32 time_skipping = 5;</code>
     * @return The timeSkipping.
     */
    @java.lang.Override
    public int getTimeSkipping() {
      return instance.getTimeSkipping();
    }
    /**
     * <code>int32 time_skipping = 5;</code>
     * @param value The timeSkipping to set.
     * @return This builder for chaining.
     */
    public Builder setTimeSkipping(int value) {
      copyOnWrite();
      instance.setTimeSkipping(value);
      return this;
    }
    /**
     * <code>int32 time_skipping = 5;</code>
     * @return This builder for chaining.
     */
    public Builder clearTimeSkipping() {
      copyOnWrite();
      instance.clearTimeSkipping();
      return this;
    }

    /**
     * <code>int32 time_listened = 6;</code>
     * @return The timeListened.
     */
    @java.lang.Override
    public int getTimeListened() {
      return instance.getTimeListened();
    }
    /**
     * <code>int32 time_listened = 6;</code>
     * @param value The timeListened to set.
     * @return This builder for chaining.
     */
    public Builder setTimeListened(int value) {
      copyOnWrite();
      instance.setTimeListened(value);
      return this;
    }
    /**
     * <code>int32 time_listened = 6;</code>
     * @return This builder for chaining.
     */
    public Builder clearTimeListened() {
      copyOnWrite();
      instance.clearTimeListened();
      return this;
    }

    // @@protoc_insertion_point(builder_scope:api.LegacyStatsResponse)
  }
  @java.lang.Override
  @java.lang.SuppressWarnings({"unchecked", "fallthrough"})
  protected final java.lang.Object dynamicMethod(
      com.google.protobuf.GeneratedMessageLite.MethodToInvoke method,
      java.lang.Object arg0, java.lang.Object arg1) {
    switch (method) {
      case NEW_MUTABLE_INSTANCE: {
        return new com.pocketcasts.service.api.LegacyStatsResponse();
      }
      case NEW_BUILDER: {
        return new Builder();
      }
      case BUILD_MESSAGE_INFO: {
          java.lang.Object[] objects = new java.lang.Object[] {
            "timesStartedAt_",
            "timeSilenceRemoval_",
            "timeVariableSpeed_",
            "timeIntroSkipping_",
            "timeSkipping_",
            "timeListened_",
          };
          java.lang.String info =
              "\u0000\u0006\u0000\u0000\u0001\u0006\u0006\u0000\u0000\u0000\u0001\u0004\u0002\u0004" +
              "\u0003\u0004\u0004\u0004\u0005\u0004\u0006\u0004";
          return newMessageInfo(DEFAULT_INSTANCE, info, objects);
      }
      // fall through
      case GET_DEFAULT_INSTANCE: {
        return DEFAULT_INSTANCE;
      }
      case GET_PARSER: {
        com.google.protobuf.Parser<com.pocketcasts.service.api.LegacyStatsResponse> parser = PARSER;
        if (parser == null) {
          synchronized (com.pocketcasts.service.api.LegacyStatsResponse.class) {
            parser = PARSER;
            if (parser == null) {
              parser =
                  new DefaultInstanceBasedParser<com.pocketcasts.service.api.LegacyStatsResponse>(
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


  // @@protoc_insertion_point(class_scope:api.LegacyStatsResponse)
  private static final com.pocketcasts.service.api.LegacyStatsResponse DEFAULT_INSTANCE;
  static {
    LegacyStatsResponse defaultInstance = new LegacyStatsResponse();
    // New instances are implicitly immutable so no need to make
    // immutable.
    DEFAULT_INSTANCE = defaultInstance;
    com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(
      LegacyStatsResponse.class, defaultInstance);
  }

  public static com.pocketcasts.service.api.LegacyStatsResponse getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static volatile com.google.protobuf.Parser<LegacyStatsResponse> PARSER;

  public static com.google.protobuf.Parser<LegacyStatsResponse> parser() {
    return DEFAULT_INSTANCE.getParserForType();
  }
}

