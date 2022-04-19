package com.example.TestMqtt.payload;

public class DevicePayload {
    private String mac;
    private int rssi;
    private String advData; // broadcast device advance data
    private long timestamp;
    private String name; // broadcast device name
    private String dev_MAC;

    public DevicePayload(String mac, int rssi, String advData, long timestamp, String dev_MAC) {
        this.mac = mac;
        this.rssi = rssi;
        this.advData = advData;
        this.timestamp = timestamp;
        this.dev_MAC = dev_MAC;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public String getAdvData() {
        return advData;
    }

    public void setAdvData(String advData) {
        this.advData = advData;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDev_MAC() {
        return dev_MAC;
    }

    public void setDev_MAC(String dev_MAC) {
        this.dev_MAC = dev_MAC;
    }

    @Override
    public String toString() {
        return "DevicePayload{" +
                "mac='" + mac + '\'' +
                ", rssi=" + rssi +
                ", advData='" + advData + '\'' +
                ", timestamp=" + timestamp +
                ", name='" + name + '\'' +
                ", dev_MAC='" + dev_MAC + '\'' +
                '}';
    }
}
