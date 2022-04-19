package com.example.TestMqtt.mqtt.broker.subscribe;


public class Subscription {

    private String clientId;

    private String topicFilter;

    public Subscription(String clientId, String topicFilter) {
        this.clientId = clientId;
        this.topicFilter = topicFilter;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getTopicFilter() {
        return topicFilter;
    }

    public void setTopicFilter(String topicFilter) {
        this.topicFilter = topicFilter;
    }

    @Override
    public String toString() {
        return "Subscription{" +
                "clientId='" + clientId + '\'' +
                ", topicFilter='" + topicFilter + '\'' +
                '}';
    }
}
