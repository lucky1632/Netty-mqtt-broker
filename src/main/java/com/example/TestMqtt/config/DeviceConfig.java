package com.example.TestMqtt.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DeviceConfig {
    int port = 5555;

    public DeviceConfig() {
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "DeviceConfig{" +
                "port=" + port +
                '}';
    }
}
