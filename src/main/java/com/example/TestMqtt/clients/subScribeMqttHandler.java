package com.example.TestMqtt.clients;

import com.example.TestMqtt.clients.client.MqttHandler;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttFixedHeader;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttMessage;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttMessageFactory;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttMessageType;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttPacketIdVariableHeader;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttQoS;
import com.example.TestMqtt.mqtt.broker.session.SessionRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;
import org.springframework.beans.factory.annotation.Autowired;

public class subScribeMqttHandler implements MqttHandler {
    @Autowired
    private SessionRegistry registry;

    @Override
    public void onMessage(String topic, ByteBuf payload) {
        System.out.println("received payload from server");
        String result = payload.toString(CharsetUtil.UTF_8);

        System.out.println(result);
//        registry.sendMessage(
//                MqttMessageFactory.newMessage(
//                        new MqttFixedHeader(MqttMessageType.PUBACK,false, MqttQoS.AT_LEAST_ONCE, false, 0),
//                        MqttPacketIdVariableHeader.from(),
//                        null
//                )
//        );
    }
}
