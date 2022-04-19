package com.example.MqttClient.client.common;


import com.example.MqttClient.io.netty.handler.codec.mqtt.MqttMessage;
import com.example.MqttClient.io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.channel.EventLoop;


import java.util.function.Consumer;

final class MqttIncomingQos2Publish {

    private final MqttPublishMessage incomingPublish;

    private final RetransmissionHandler<MqttMessage> retransmissionHandler = new RetransmissionHandler<>();

    MqttIncomingQos2Publish(MqttPublishMessage incomingPublish, MqttMessage originalMessage) {
        this.incomingPublish = incomingPublish;

        this.retransmissionHandler.setOriginalMessage(originalMessage);
    }

    MqttPublishMessage getIncomingPublish() {
        return incomingPublish;
    }

    void startPubrecRetransmitTimer(EventLoop eventLoop, Consumer<Object> sendPacket) {
        this.retransmissionHandler.setHandle((fixedHeader, originalMessage) ->
                sendPacket.accept(new MqttMessage(fixedHeader, originalMessage.variableHeader())));
        this.retransmissionHandler.start(eventLoop);
    }

    void onPubrelReceived() {
        this.retransmissionHandler.stop();
    }
}
