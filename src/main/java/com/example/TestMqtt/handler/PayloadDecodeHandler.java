package com.example.TestMqtt.handler;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.example.TestMqtt.payload.DeviceMessageInMqtt;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

public class PayloadDecodeHandler extends MessageToMessageDecoder<DeviceMessageInMqtt> {

    private Meter meter;
    public PayloadDecodeHandler(MetricRegistry metricRegistry) {
        meter = metricRegistry.meter("device");
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, DeviceMessageInMqtt deviceMessageInMqtt, List<Object> list) throws Exception {
        System.out.println("Received Message : "+ deviceMessageInMqtt);

//        list.add(deviceMessageInMqtt);
    }
}
