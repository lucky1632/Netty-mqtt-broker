package com.example.TestMqtt.mqtt.util;


import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttConnAckVariableHeader;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttConnectPayload;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttPacketIdVariableHeader;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttSubAckPayload;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttUnsubscribePayload;
import com.example.TestMqtt.mqtt.api.message.MqttPublishPayload;
import com.example.TestMqtt.mqtt.api.message.MqttSubscribePayloadGranted;
import com.example.TestMqtt.mqtt.api.message.Message;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/** JSON Utils */
public class JSONs {

  // Global JSON ObjectMapper
  public static final ObjectMapper Mapper = new ObjectMapper();

  static {
    Mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//    Mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
//    Mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
    Mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    Mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
  }

  private JSONs() {}

  /** Decode bytes (json data) to Message */
  public static Message decodeMessage(byte[] data) throws IOException {
    JavaType type =
        Mapper.getTypeFactory()
            .constructParametricType(Message.class, JsonNode.class, JsonNode.class);
    Message<JsonNode, JsonNode> m = Mapper.readValue(data, type);
    switch (m.fixedHeader().messageType()) {
      case CONNECT:
        MqttConnectVariableHeader cv =
            Mapper.treeToValue(m.variableHeader(), MqttConnectVariableHeader.class);
        MqttConnectPayload cp = Mapper.treeToValue(m.payload(), MqttConnectPayload.class);
        return new Message<>(m.fixedHeader(), m.additionalHeader(), cv, cp);
      case CONNACK:
        MqttConnAckVariableHeader cav =
            Mapper.treeToValue(m.variableHeader(), MqttConnAckVariableHeader.class);
        return new Message<>(m.fixedHeader(), m.additionalHeader(), cav, null);
      case SUBSCRIBE:
        MqttPacketIdVariableHeader sv =
            Mapper.treeToValue(m.variableHeader(), MqttPacketIdVariableHeader.class);
        MqttSubscribePayloadGranted sp =
            Mapper.treeToValue(m.payload(), MqttSubscribePayloadGranted.class);
        return new Message<>(m.fixedHeader(), m.additionalHeader(), sv, sp);
      case SUBACK:
        MqttPacketIdVariableHeader sav =
            Mapper.treeToValue(m.variableHeader(), MqttPacketIdVariableHeader.class);
        MqttSubAckPayload sap = Mapper.treeToValue(m.payload(), MqttSubAckPayload.class);
        return new Message<>(m.fixedHeader(), m.additionalHeader(), sav, sap);
      case UNSUBSCRIBE:
        MqttPacketIdVariableHeader uv =
            Mapper.treeToValue(m.variableHeader(), MqttPacketIdVariableHeader.class);
        MqttUnsubscribePayload up = Mapper.treeToValue(m.payload(), MqttUnsubscribePayload.class);
        return new Message<>(m.fixedHeader(), m.additionalHeader(), uv, up);
      case PUBLISH:
        MqttPublishVariableHeader pv =
            Mapper.treeToValue(m.variableHeader(), MqttPublishVariableHeader.class);
        MqttPublishPayload pp = Mapper.treeToValue(m.payload(), MqttPublishPayload.class);
        return new Message<>(m.fixedHeader(), m.additionalHeader(), pv, pp);
      case UNSUBACK:
      case PUBACK:
      case PUBREC:
      case PUBREL:
      case PUBCOMP:
        MqttPacketIdVariableHeader iv =
            Mapper.treeToValue(m.variableHeader(), MqttPacketIdVariableHeader.class);
        return new Message<>(m.fixedHeader(), m.additionalHeader(), iv, null);
      case PINGREQ:
      case PINGRESP:
      case DISCONNECT:
        return m;
      default:
        return null;
    }
  }
}
