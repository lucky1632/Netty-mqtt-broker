package com.example.TestMqtt.mqtt.api.message;


import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttGrantedQoS;
import io.netty.util.internal.StringUtil;

/**
 * Contains a topic name and granted Qos Level. This is part of the {@link
 * MqttSubscribePayloadGranted}
 */
public class MqttTopicSubscriptionGranted {

  protected String topic;
  protected MqttGrantedQoS grantedQos;

  private MqttTopicSubscriptionGranted() {}

  public MqttTopicSubscriptionGranted(String topic, MqttGrantedQoS grantedQos) {
    this.topic = topic;
    this.grantedQos = grantedQos;
  }

  public String topic() {
    return topic;
  }

  public MqttGrantedQoS grantedQos() {
    return grantedQos;
  }

  @Override
  public String toString() {
    return StringUtil.simpleClassName(this)
        + '['
        + "topic="
        + topic
        + ", grantedQos="
        + grantedQos
        + ']';
  }
}
