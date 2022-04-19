package com.example.TestMqtt.mqtt.security;

import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttGrantedQoS;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttTopicSubscription;
import com.example.TestMqtt.mqtt.api.auth.Authenticator;
import com.example.TestMqtt.mqtt.api.auth.AuthorizeResult;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

public class DummyAuthenticatorImpl implements Authenticator {

    private boolean allowDollar; // allow $ in topic
    private String deniedTopic; // topic will be rejected

    @PostConstruct
    public void init() {
        this.allowDollar = true;
        this.deniedTopic = null;
    }

    @Override
    public void destroy() {}

    @Override
    public AuthorizeResult authConnect(String clientId, String userName, String password) {
        return AuthorizeResult.OK;
    }

    @Override
    public AuthorizeResult authPublish(
            String clientId, String userName, String topicName, int qos, boolean retain) {
        if (!this.allowDollar && topicName.startsWith("$")) return AuthorizeResult.FORBIDDEN;
        if (topicName.equals(this.deniedTopic)) return AuthorizeResult.FORBIDDEN;
        return AuthorizeResult.OK;
    }

    @Override
    public List<MqttGrantedQoS> authSubscribe(
            String clientId, String userName, List<MqttTopicSubscription> requestSubscriptions) {
        List<MqttGrantedQoS> r = new ArrayList<>();
        requestSubscriptions.forEach(
                subscription -> {
                    if (!this.allowDollar && subscription.topic().startsWith("$"))
                        r.add(MqttGrantedQoS.NOT_GRANTED);
                    else if (subscription.topic().equals(this.deniedTopic)) r.add(MqttGrantedQoS.NOT_GRANTED);
                    else r.add(MqttGrantedQoS.valueOf(subscription.requestedQos().value()));
                });
        return r;
    }

    @Override
    public String oauth(String credentials) {
        return "dummy";
    }
}

