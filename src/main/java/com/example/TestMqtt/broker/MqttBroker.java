package com.example.TestMqtt.broker;

import com.codahale.metrics.MetricRegistry;
import com.example.TestMqtt.config.ConfigProperties;
import com.example.TestMqtt.config.DeviceConfig;
import com.example.TestMqtt.handler.MqttProtocolHandler;
import com.example.TestMqtt.handler.PayloadDecodeHandler;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttDecoder;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttEncoder;
import com.example.TestMqtt.mqtt.broker.session.SessionRegistry;
import com.example.TestMqtt.mqtt.broker.subscribe.SubscribeService;
import com.example.TestMqtt.mqtt.broker.util.Validator;
import com.example.TestMqtt.mqtt.security.DummyAuthenticatorImpl;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
public class MqttBroker {

    private int port;
    private Channel channel = null;
    MetricRegistry metricRegistry = new MetricRegistry();

    @Autowired ConfigProperties properties;
    @Autowired SessionRegistry registry;
    @Autowired
    TaskScheduler scheduler;

    @Autowired
    SubscribeService subscribeService;

    @Autowired DeviceConfig config;

    public MqttBroker() {
    }

    @PostConstruct
    public void init() throws Exception {
        this.port = config.getPort();
        initSettings();
    }

    public void startServer(){
        System.out.println("started broker");
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(MqttBroker.class, LogLevel.TRACE))
                .childHandler(
                        new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel socketChannel) throws Exception {
                                ChannelPipeline pipe = socketChannel.pipeline();
                                pipe.addFirst("idleHandler", new IdleStateHandler(0,0,120));
                                pipe.addLast("decoder", new MqttDecoder());
                                pipe.addLast("encoder", MqttEncoder.INSTANCE);
                                pipe.addLast("mqttHandler",
                                        new MqttProtocolHandler(
                                                properties,
                                                new DummyAuthenticatorImpl(),
                                                registry,
                                                new Validator(),
                                                subscribeService, properties.getMqtt().getBroker_id(),
                                                120,
                                                65535,
                                                scheduler)
                                );
                                pipe.addLast("decodedpayload", new PayloadDecodeHandler(metricRegistry));
                            }
                        }
                ).option(ChannelOption.SO_BACKLOG, 511)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
        channel = serverBootstrap.bind(port).syncUninterruptibly().channel();
        System.out.println("server is up and running on port :"+ port);
    }

    @Scheduled(fixedRateString = "${push.pollingFrequency:10000}", initialDelay = 20000)
    public void initSettings() throws Exception {
        if (channel == null) {
            port = config.getPort();
            startServer();
        } else {
            if (config.getPort() != port) {
                destroy();
                port = config.getPort();
                startServer();
            }
        }
    }

    @PreDestroy
    void destroy() {
        if (channel != null) {
            channel.flush();
            //LOGGER.debug("{} MQTT broker is shutting down ...", device);
            channel.close().syncUninterruptibly();
            //LOGGER.info("Parent: {}", channel.parent());
            channel = null;
            //LOGGER.debug("{} MQTT broker shut down ...", device);
            System.out.println("MQTT broker shut down ...");
        }
    }
}
