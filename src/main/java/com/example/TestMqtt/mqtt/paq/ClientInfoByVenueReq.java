package com.example.TestMqtt.mqtt.paq;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class ClientInfoByVenueReq extends Paq {

  protected String venue;

  public ClientInfoByVenueReq(String venue) {
    super.type = 0x1;
    super.subtype = 0x4;
    this.venue = venue;
  }

  @Override
  public ByteBuf toByteBuf() {
    ByteBuf buf = Unpooled.buffer();
    buf.writeByte(type);
    buf.writeByte(subtype);
    buf.writeShort(16);

    byte[] s = new byte[16];
    byte[] ss = venue.getBytes();
    for (int n = 0; n < ss.length; n++) {
      s[n] = ss[n];
    }
    buf.writeBytes(s);
    return buf;
  }

  @Override
  public String toString() {
    return "ClientInfoByVenueReq [venue=" + venue + "]";
  }
}
