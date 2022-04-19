package com.example.TestMqtt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.security.SecureRandom;

@ConfigurationProperties(prefix = "push", ignoreUnknownFields = false)
public class ConfigProperties {

    private int pollingFrequency = 10000;

    private DeviceConfig deviceConfig = new DeviceConfig();

    public DeviceConfig getDeviceConfig() {
        return deviceConfig;
    }

    public void setDeviceConfig(DeviceConfig deviceConfig) {
        this.deviceConfig = deviceConfig;
    }

    public class Mqtt {

        private String psk_id = generateRandomString(5), psk_key = generateRandomString(8);
        int keepAlive = 120, keepAliveMax = 65535;
        boolean assoc = true;
        boolean info = true;
        boolean probing = true;
        int scheduleFreq = 60;
        int clientsFreq = 60;

        public int getClientsFreq() {
            return clientsFreq;
        }

        public void setClientsFreq(int clientsFreq) {
            this.clientsFreq = clientsFreq;
        }

        public boolean getAssoc() {
            return assoc;
        }

        public void setAssoc(boolean assoc) {
            this.assoc = assoc;
        }

        public boolean getInfo() {
            return info;
        }

        public void setInfo(boolean info) {
            this.info = info;
        }

        @Override
        public String toString() {
            return "Mqtt [psk_id="
                    + psk_id
                    + ", psk_key="
                    + psk_key
                    + ", keepAlive="
                    + keepAlive
                    + ", keepAliveMax="
                    + keepAliveMax
                    + ", assoc="
                    + assoc
                    + ", info="
                    + info
                    + ", probing="
                    + probing
                    + ", scheduleFreq="
                    + scheduleFreq
                    + ", clientsFreq="
                    + clientsFreq
                    + ", broker_id="
                    + broker_id
                    + "]";
        }

        public boolean getProbing() {
            return probing;
        }

        public void setProbing(boolean probing) {
            this.probing = probing;
        }

        public int getScheduleFreq() {
            return scheduleFreq;
        }

        public void setScheduleFreq(int scheduleFreq) {
            this.scheduleFreq = scheduleFreq;
        }

        private String broker_id = "Mqtt Analytics";

        public String getPsk_key() {
            return psk_key;
        }

        public void setPsk_key(String psk_key) {
            this.psk_key = psk_key;
        }

        public String getPsk_id() {
            return psk_id;
        }

        public void setPsk_id(String psk_id) {
            this.psk_id = psk_id;
        }

        public String getBroker_id() {
            return broker_id;
        }

        public void setBroker_id(String broker_id) {
            this.broker_id = broker_id;
        }

        public int getKeepAlive() {
            return keepAlive;
        }

        public void setKeepAlive(int keepAlive) {
            this.keepAlive = keepAlive;
        }

        public int getKeepAliveMax() {
            return keepAliveMax;
        }

        public void setKeepAliveMax(int keepAliveMax) {
            this.keepAliveMax = keepAliveMax;
        }

    }

    private Mqtt mqtt = new Mqtt();

    public Mqtt getMqtt() {
        return mqtt;
    }

    public void setMqtt(Mqtt mqtt) {
        this.mqtt = mqtt;
    }

    static String generateRandomString(final int keyLen) {
        SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[keyLen];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    public static String generateMac(final int keyLen) {
        SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[keyLen];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X:", b));
        }
        return sb.substring(0, sb.length() - 1);
    }
}
