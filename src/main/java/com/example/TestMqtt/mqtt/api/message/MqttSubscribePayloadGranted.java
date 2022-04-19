package com.example.TestMqtt.mqtt.api.message;

import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.util.internal.StringUtil;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;

/** Payload of the {@link MqttSubscribeMessage} After subscription was granted */
public class MqttSubscribePayloadGranted {

  protected List<MqttTopicSubscriptionGranted> subscriptions;

  private MqttSubscribePayloadGranted() {}

  public MqttSubscribePayloadGranted(List<MqttTopicSubscriptionGranted> subscriptions) {
    this.subscriptions = subscriptions;
  }

  public List<MqttTopicSubscriptionGranted> subscriptions() {
    return subscriptions;
  }

  @Override
  public String toString() {
    return StringUtil.simpleClassName(this)
        + '['
        + "subscriptions="
        + ArrayUtils.toString(subscriptions)
        + ']';
  }
}
