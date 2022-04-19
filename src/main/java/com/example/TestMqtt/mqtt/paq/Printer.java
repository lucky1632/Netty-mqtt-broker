package com.example.TestMqtt.mqtt.paq;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class Printer extends SimpleChannelInboundHandler<Paq> {

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Paq msg) throws Exception {
    System.err.println(msg.toString());
  }
}
