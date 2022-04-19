package com.example.MqttClient.client.handler;

import com.example.MqttClient.client.common.MqttHandler;
import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

public class subscriptionHandler implements MqttHandler {
    @Override
    public void onMessage(String topic, ByteBuf payload) {
        System.out.println("received payload from server");
        String result = payload.toString(CharsetUtil.UTF_8);

        System.out.println(result);
    }
}
