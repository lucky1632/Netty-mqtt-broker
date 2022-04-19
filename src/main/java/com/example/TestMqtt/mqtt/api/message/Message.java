package com.example.TestMqtt.mqtt.api.message;

import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttFixedHeader;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttGrantedQoS;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttMessage;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttMessageFactory;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttPacketIdVariableHeader;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttPublishMessage;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttQoS;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttSubscribePayload;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttTopicSubscription;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.internal.StringUtil;

import java.util.ArrayList;
import java.util.List;

/** Message */
public class Message<V, P> {

  protected MqttFixedHeader fixedHeader;
  protected MqttAdditionalHeader additionalHeader;
  protected V variableHeader;
  protected P payload;

  private Message() {}

  public Message(
      MqttFixedHeader fixedHeader,
      MqttAdditionalHeader additionalHeader,
      V variableHeader,
      P payload) {
    this.fixedHeader = fixedHeader;
    this.additionalHeader = additionalHeader;
    this.variableHeader = variableHeader;
    this.payload = payload;
  }

  public static Message fromMqttMessage(
          MqttMessage msg, MqttVersion version, String clientId, String userName, String brokerId) {
    switch (msg.fixedHeader().messageType()) {
      case CONNECT:
      case CONNACK:
      case SUBACK:
      case UNSUBSCRIBE:
      case UNSUBACK:
      case PUBACK:
      case PUBREC:
      case PUBREL:
      case PUBCOMP:
      case PINGREQ:
      case PINGRESP:
      case DISCONNECT:
        return new Message<>(
            msg.fixedHeader(),
            new MqttAdditionalHeader(version, clientId, userName, brokerId),
            msg.variableHeader(),
            msg.payload());
      default:
        throw new IllegalArgumentException(
            "unknown message type " + msg.fixedHeader().messageType());
    }
  }

  public static Message<MqttPublishVariableHeader, MqttPublishPayload> fromMqttMessage(
      MqttPublishMessage msg,
      MqttVersion version,
      String clientId,
      String userName,
      String brokerId) {
    // forge bytes payload
    byte[] bytes = new byte[0];
    if (msg.payload() != null && msg.payload().readableBytes() > 0) {
      ByteBuf buf = msg.payload().duplicate();
      bytes = new byte[buf.readableBytes()];
      buf.readBytes(bytes);
    }
    return new Message<>(
        msg.fixedHeader(),
        new MqttAdditionalHeader(version, clientId, userName, brokerId),
        msg.variableHeader(),
        new MqttPublishPayload(bytes));
  }

  public static Message<MqttPacketIdVariableHeader, MqttSubscribePayloadGranted> fromMqttMessage(
      MqttSubscribeMessage msg,
      List<MqttGrantedQoS> grantedQoSes,
      MqttVersion version,
      String clientId,
      String userName,
      String brokerId) {
    // forge topic subscriptions
    List<MqttTopicSubscriptionGranted> subscriptions = new ArrayList<>();
    for (int i = 0; i < msg.payload().subscriptions().size(); i++) {
      MqttTopicSubscriptionGranted subscription =
          new MqttTopicSubscriptionGranted(
              msg.payload().subscriptions().get(i).topic(), grantedQoSes.get(i));
      subscriptions.add(subscription);
    }
    return new Message<>(
        msg.fixedHeader(),
        new MqttAdditionalHeader(version, clientId, userName, brokerId),
        msg.variableHeader(),
        new MqttSubscribePayloadGranted(subscriptions));
  }

  public MqttFixedHeader fixedHeader() {
    return fixedHeader;
  }

  public MqttAdditionalHeader additionalHeader() {
    return additionalHeader;
  }

  public V variableHeader() {
    return variableHeader;
  }

  public P payload() {
    return payload;
  }

  public MqttMessage toMqttMessage() {
    switch (fixedHeader.messageType()) {
      case CONNECT:
      case CONNACK:
      case SUBACK:
      case UNSUBSCRIBE:
        return MqttMessageFactory.newMessage(fixedHeader, variableHeader, payload);
      case UNSUBACK:
      case PUBACK:
      case PUBREC:
      case PUBREL:
      case PUBCOMP:
        return MqttMessageFactory.newMessage(fixedHeader, variableHeader, null);
      case PINGREQ:
      case PINGRESP:
      case DISCONNECT:
        return MqttMessageFactory.newMessage(fixedHeader, null, null);
      case SUBSCRIBE:
        List<MqttTopicSubscription> subscriptions = new ArrayList<>();
        ((MqttSubscribePayloadGranted) payload)
            .subscriptions()
            .forEach(
                s -> {
                  if (s.grantedQos != MqttGrantedQoS.NOT_GRANTED) {
                    subscriptions.add(
                        new MqttTopicSubscription(s.topic, MqttQoS.valueOf(s.grantedQos.value())));
                  }
                });
        return MqttMessageFactory.newMessage(
            fixedHeader, variableHeader, new MqttSubscribePayload(subscriptions));
      case PUBLISH:
        MqttPublishPayload p = (MqttPublishPayload) payload;
        return MqttMessageFactory.newMessage(
            fixedHeader,
            variableHeader,
            (p != null && p.bytes != null && p.bytes.length > 0)
                ? Unpooled.wrappedBuffer(p.bytes)
                : Unpooled.EMPTY_BUFFER);
      default:
        throw new IllegalStateException("unknown message type " + fixedHeader.messageType());
    }
  }

  @Override
  public String toString() {
    return StringUtil.simpleClassName(this)
        + '['
        + "fixedHeader="
        + (fixedHeader != null ? fixedHeader.toString() : "")
        + ", additionalHeader="
        + (additionalHeader != null ? additionalHeader.toString() : "")
        + ", variableHeader="
        + (variableHeader != null ? variableHeader.toString() : "")
        + ", payload="
        + (payload != null ? payload.toString() : "")
        + ']';
  }
}
