package com.example.TestMqtt.clients;

import com.example.TestMqtt.clients.client.MqttClient;
import com.example.TestMqtt.clients.client.MqttClientCallback;
import com.example.TestMqtt.clients.client.MqttClientImpl;
import com.example.TestMqtt.clients.client.MqttConnectResult;
import com.example.TestMqtt.clients.client.MqttHandler;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttDecoder;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttEncoder;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttFixedHeader;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttMessage;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttMessageFactory;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttMessageType;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttQoS;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class TestClient {
    private String host;
    private int port;

    private static Boolean publish = false;

    public TestClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static void main(String[] args) throws InterruptedException {
        EventLoopGroup loop = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);

        MqttClient mqttClient = new MqttClientImpl(((topic, payload) -> {
            System.out.println(topic + "=>" + payload.toString(StandardCharsets.UTF_8));
        }));

        mqttClient.setEventLoop(loop);
        mqttClient.getClientConfig().setChannelClass(NioSocketChannel.class);
        mqttClient.getClientConfig().setClientId("asdfkjkljf267"); // you can use shortUuid() to generate random clientId
//        mqttClient.getClientConfig().setUsername("test1");
//        mqttClient.getClientConfig().setPassword("test");
        mqttClient.getClientConfig().setProtocolVersion(MqttVersion.MQTT_3_1_1);
        mqttClient.getClientConfig().setReconnect(false);
        mqttClient.setCallback(new MqttClientCallback() {
            @Override
            public void connectionLost(Throwable cause) {

                cause.printStackTrace();
            }

            @Override
            public void onSuccessfulReconnect() {

            }
        });

        /**
         * publish 3 document to the server with port number 5555
         * then disconnect the client from server
         */
//        if(publish){
//            mqttClient.connect("127.0.0.1", 5555)
//                    .addListener(future -> {
//                        try {
//                            MqttConnectResult result = (MqttConnectResult) future.get(15, TimeUnit.SECONDS);
//                            if (result.getReturnCode() != MqttConnectReturnCode.CONNECTION_ACCEPTED) {
//                                System.out.println("error:" + result.getReturnCode() + "--");
//                                mqttClient.disconnect();
//                                return;
//                            }
//                            System.out.println("Connected to the Broker");
//                            String payload = "{ \"mac\" :\"2C:54:91:88:5:E3\" , \"rssi\" : -58 , \"advData\": \"kjfhskdjhfkj\" , \"timestamp\" : 7362767 }";
//                            int count = 0;
//                            // just sending 2 message and terminating it
//                            while(count++ < 2){
//                                mqttClient.publish("gliot/suydfiuh3iuu45", Unpooled.copiedBuffer(ByteBuffer.wrap(payload.getBytes(StandardCharsets.UTF_8))));
//                            }
//                            mqttClient.disconnect();
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }).await(5, TimeUnit.SECONDS);
//        }

        /**
         * subscribe for the topic "gliot"
         */
        mqttClient.connect("127.0.0.1", 5555)
                .addListener(future -> {
                    try{
                        MqttConnectResult result = (MqttConnectResult) future.get(60, TimeUnit.SECONDS);
                        if(result.getReturnCode() != MqttConnectReturnCode.CONNECTION_ACCEPTED){
                            System.out.println("error : "+ result.getReturnCode() + "--");
                            mqttClient.disconnect();
                            return;
                        }
                        System.out.println("Connected to MQTT Broker ");
                        mqttClient.on("gliot/tz", new subScribeMqttHandler()).addListener((subFuture) -> {
                            System.out.println("Received Acknowledgement for the subscription............");
                        });

                    }catch (Exception e){
                        e.printStackTrace();
                    }
                });
    }
}
