package com.example.TestMqtt.payload;

import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

public class DeviceMessageInMqtt {

    private String topicName;
    private DevicePayload payload;

    public DeviceMessageInMqtt(String topicName, DevicePayload payload) {
        this.topicName = topicName;
        this.payload = payload;
    }

    public static DeviceMessageInMqtt of(ByteBuf content, String topic) {
        int size = content.readableBytes();
        content.markReaderIndex();
        byte[] msg = new byte[size];
        content.getBytes(0, msg);
        String s = new String(msg, StandardCharsets.UTF_8);
        //System.out.println("Received message : "+ s +" from " + topic);

        content.resetReaderIndex();
        Gson gson = new Gson();
        DevicePayload payload = gson.fromJson(s, DevicePayload.class);
        String[] topics = topic.split("/");
        payload.setDev_MAC(topics[topics.length-1]);
        System.out.println("Received message : "+ payload.toString() +" from " + topic);

        return new DeviceMessageInMqtt(topic, payload);
    }

    public String getTopicName() {
        return topicName;
    }

    public DevicePayload getPayload() {
        return payload;
    }

}
