package com.example.TestMqtt.mqtt.paq;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

public class DummyPag extends Paq {

  protected String data;

  public DummyPag(ByteBuf content) {
    length = content.readUnsignedShort();
    data = ByteBufUtil.hexDump(content);
  }

  @Override
  public ByteBuf toByteBuf() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String toString() {
    return "DummyPag [type= "
        + type
        + ", subtype="
        + subtype
        + ", length="
        + length
        + ", data :"
        + data
        + "]";
  }
}
