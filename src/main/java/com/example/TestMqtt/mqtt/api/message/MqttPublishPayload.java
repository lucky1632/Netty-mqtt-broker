package com.example.TestMqtt.mqtt.api.message;

import io.netty.buffer.ByteBuf;
import io.netty.util.internal.StringUtil;

/** Mqtt Publish Payload with byte[] */
public class MqttPublishPayload {

  protected byte[] bytes;

  private MqttPublishPayload() {}

  public MqttPublishPayload(byte[] bytes) {
    this.bytes = bytes;
  }

  public MqttPublishPayload(ByteBuf buf) {
    ByteBuf b = buf.duplicate();
    this.bytes = new byte[b.readableBytes()];
    b.readBytes(this.bytes);
  }

  public byte[] bytes() {
    return bytes;
  }

  @Override
  public String toString() {
    return StringUtil.simpleClassName(this) + '[' + "payload " + bytes.length + " bytes" + ']';
  }
}
