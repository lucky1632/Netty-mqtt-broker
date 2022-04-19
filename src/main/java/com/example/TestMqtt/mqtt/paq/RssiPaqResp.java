package com.example.TestMqtt.mqtt.paq;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

public class RssiPaqResp extends Paq {

  private long timestamp;
  private String apMac;
  private int band;
  private int airtime;

  private List<ClientInfo> clientInfos = new ArrayList();

  public static class ClientInfo {
    String clientMac;
    int rssi0, rssi1, rssi2;

    public ClientInfo(String clientMac, int rssi0, int rssi1, int rssi2) {
      super();
      this.clientMac = clientMac;
      this.rssi0 = (rssi0 == -128) ? -1 : rssi0;
      this.rssi1 = (rssi1 == -128) ? -1 : rssi1;
      this.rssi2 = (rssi2 == -128) ? -1 : rssi2;
    }

    public String getClientMac() {
      return clientMac;
    }

    public int getRssi0() {
      return rssi0;
    }

    public int getRssi1() {
      return rssi1;
    }

    public int getRssi2() {
      return rssi2;
    }

    @Override
    public String toString() {
      return "ClientInfo [clientMac="
          + clientMac
          + ", rssi0="
          + rssi0
          + ", rssi1="
          + rssi1
          + ", rssi2="
          + rssi2
          + "]\n";
    }
  }

  public RssiPaqResp(ByteBuf content) {
    int code = content.readShort();
    if (code != 0x0403) throw new IllegalArgumentException("Invalid header");
    int length = content.readShort();
    timestamp = content.readUnsignedInt();
    long ignore = content.readUnsignedInt();
    apMac = readMac(content);
    band = content.readByte();
    airtime = content.readByte();
    int clientCount = content.readShort();

    for (int c = 0; c < clientCount; c++) {
      String clientMac = readMac(content);
      int phyinfo = content.readByte();
      int rssi0 = content.readByte();
      int rssi1 = content.readByte();
      int rssi2 = content.readByte();
      int nf = content.readByte();
      clientInfos.add(new ClientInfo(clientMac, rssi0, rssi1, rssi2));
    }
  }

  @Override
  public ByteBuf toByteBuf() {
    return null;
  }

  @Override
  public String toString() {
    return "RssiPaqResp [timestamp="
        + timestamp
        + ", apMac="
        + apMac
        + ", band="
        + band
        + ", airtime="
        + airtime
        + ", clientInfos="
        + clientInfos
        + "]";
  }

  public long getTimestamp() {
    return timestamp;
  }

  public String getApMac() {
    return apMac;
  }

  public int getBand() {
    return band;
  }

  public int getAirtime() {
    return airtime;
  }

  public List<ClientInfo> getClientInfos() {
    return clientInfos;
  }
}
