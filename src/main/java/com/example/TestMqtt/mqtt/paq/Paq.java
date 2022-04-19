package com.example.TestMqtt.mqtt.paq;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Paq {

  protected static Logger LOGGER = LoggerFactory.getLogger(Paq.class);

  protected int type;
  protected int subtype;
  protected int length;

  protected String readMac(ByteBuf buf) {
    byte bytes[] = new byte[6];
    buf.readBytes(bytes);
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02X:", b));
    }
    return sb.substring(0, sb.length() - 1);
  }

  protected String getIp4(ByteBuf buf) {
    byte bytes[] = new byte[4];
    buf.readBytes(bytes);
    StringBuilder sb = new StringBuilder();
    for (int n = bytes.length - 1; n >= 0; n--) {
      sb.append(String.format("%d.", bytes[n]));
    }
    return sb.substring(0, sb.length() - 1);
  }

  protected String nullTerminatedString(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length);
    for (byte b : bytes) {
      if (b == 0) break;
      sb.append((char) b);
    }
    return sb.toString();
  }

  public abstract ByteBuf toByteBuf();

  public static Paq of(ByteBuf content) {
    int selector = content.getShort(0);

    switch (selector) {
      case 0x0204:
        return new ClientInfoByVenueResp(content);
      case 0x0403:
        return new RssiPaqResp(content);
      default:
        LOGGER.info("+++++++++++++++++Not Handing: {}", ByteBufUtil.prettyHexDump(content));
        return new RssiPaqResp(content);
    }
  }
}
