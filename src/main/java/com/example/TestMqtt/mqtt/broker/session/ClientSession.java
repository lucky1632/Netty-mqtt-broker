package com.example.TestMqtt.mqtt.broker.session;

import io.netty.channel.Channel;

public class ClientSession {

    private String clientId;

    private Channel channel;

    private boolean cleanSession;

    public ClientSession(String clientId, Channel channel, boolean cleanSession) {
        this.channel = channel;
        this.clientId = clientId;
        this.cleanSession = cleanSession;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public boolean isCleanSession() {
        return cleanSession;
    }

    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    @Override
    public String toString() {
        return "ClientSession{" +
                "clientId='" + clientId + '\'' +
                ", channel=" + channel +
                ", cleanSession=" + cleanSession +
                '}';
    }
}
