/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.example.TestMqtt.io.netty.handler.codec.mqtt;

import io.netty.util.CharsetUtil;

/** Mqtt version specific constant values used by multiple classes in mqtt-codec. */
public enum MqttVersion {
  MQTT_3_1("MQIsdp", (byte) 3),
  MQTT_3_1_1("MQTT", (byte) 4);

  private final String name;
  private final byte level;

  MqttVersion(String protocolName, byte protocolLevel) {
    this.name = protocolName;
    this.level = protocolLevel;
  }

  public static MqttVersion fromProtocolNameAndLevel(String protocolName, byte protocolLevel) {
    for (MqttVersion mv : values()) {
      if (mv.name.equals(protocolName)) {
        return mv;
        //        if (mv.level == protocolLevel) {
        //          return mv;
        //        } else {
        //          throw new MqttUnacceptableProtocolVersionException(
        //              protocolName + " and " + protocolLevel + " are not match");
        //        }
      }
    }
    throw new MqttUnacceptableProtocolVersionException(protocolName + "is unknown protocol name");
  }

  public static MqttVersion fromProtocolLevel(byte protocolLevel) {
    for (MqttVersion mv : values()) {
      if (mv.level == protocolLevel) {
        return mv;
      }
    }
    throw new MqttUnacceptableProtocolVersionException(protocolLevel + "is unknown protocol level");
  }

  public String protocolName() {
    return name;
  }

  public byte[] protocolNameBytes() {
    return name.getBytes(CharsetUtil.UTF_8);
  }

  public byte protocolLevel() {
    return level;
  }
}
