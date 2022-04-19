package com.example.MqttClient.client;

import com.example.MqttClient.client.common.MqttClient;
import com.example.MqttClient.client.common.MqttClientCallback;
import com.example.MqttClient.client.common.MqttClientImpl;
import com.example.MqttClient.client.common.MqttConnectResult;
import com.example.MqttClient.client.handler.subscriptionHandler;
import com.example.MqttClient.io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import com.example.MqttClient.io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static com.example.MqttClient.io.netty.handler.codec.mqtt.UUIDs.shortUuid;

public class TestClient {
    private String host;
    private int port;

    private static Boolean publish = false;

    public TestClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static void main(String[] args) throws InterruptedException {
//        new TestClient("localhost", 5555).run();
        EventLoopGroup loop = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);

        MqttClient mqttClient = new MqttClientImpl(((topic, payload) -> {
            System.out.println(topic + "=>" + payload.toString(StandardCharsets.UTF_8));
        }));

        mqttClient.setEventLoop(loop);
        mqttClient.getClientConfig().setChannelClass(NioSocketChannel.class);
        mqttClient.getClientConfig().setClientId(shortUuid()); // generate random clientId
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
                        mqttClient.on("gliot/ty", new subscriptionHandler()).addListener((subFuture) -> {
                            System.out.println("Received Acknowledgement for the subscription");
                        });

                    }catch (Exception e){
                        e.printStackTrace();
                    }
                });
    }
}
