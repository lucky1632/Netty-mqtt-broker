package com.example.TestMqtt.mqtt.paq;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

public class RssiSubscribe extends Paq {

  int anon = 0;

  public RssiSubscribe(int subtype, int anon) {
    super.type = 0x3;
    super.subtype = subtype;
    this.anon = anon;
  }

  @Override
  public ByteBuf toByteBuf() {
    ByteBuf buf = Unpooled.buffer();
    buf.writeByte(type);
    buf.writeByte(subtype);
    buf.writeShort(7);
    // period
    buf.writeByte(6);
    // duration?
    buf.writeByte(5);
    buf.writeByte(0xa0);
    // band
    buf.writeByte(2);
    // anon
    buf.writeByte(anon);
    // count
    buf.writeByte(0);
    buf.writeByte(0);
    return buf;
  }

  @Override
  public String toString() {
    return "PassiveSubscribe [" + ByteBufUtil.hexDump(toByteBuf()) + "]";
  }
}
