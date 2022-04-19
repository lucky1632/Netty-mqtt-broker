package com.example.TestMqtt.mqtt.paq;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

public class ClientInfoByVenueResp extends Paq {

  public class ClientInfo {
    public ClientInfo(String clientMac, String os, String apMac, String ssid, String machineName) {
      super();
      this.clientMac = clientMac;
      this.os = os;
      this.apMac = apMac;
      this.ssid = ssid;
      this.machineName = machineName;
    }

    private String clientMac;
    private String os;
    private String apMac;
    private String ssid;
    private String machineName;

    public String getClientMac() {
      return clientMac;
    }

    public String getOs() {
      return os;
    }

    public String getApMac() {
      return apMac;
    }

    public String getSsid() {
      return ssid;
    }

    public String getMachineName() {
      return machineName;
    }

    @Override
    public String toString() {
      return "ClientInfo [clientMac="
          + clientMac
          + ", os="
          + os
          + ", apMac="
          + apMac
          + ", ssid="
          + ssid
          + ", machineName="
          + machineName
          + "]";
    }
  }

  List<ClientInfo> clientInfos = new ArrayList<>();

  public ClientInfoByVenueResp(ByteBuf content) {
    int code = content.readShort();
    if (code != 0x0204) throw new IllegalArgumentException("Invalid header");

    int ii = content.readShort();
    int clientCount = content.readShort();

    for (int c = 0; c < clientCount; c++) {
      String clientMac = readMac(content);
      String ip4 = getIp4(content);
      byte[] noIdea = new byte[16];
      content.readBytes(noIdea);
      byte[] os = new byte[0x40];
      content.readBytes(os);
      byte[] name = new byte[0x80];
      content.readBytes(name);
      String apMac = readMac(content);
      int band = content.readByte();
      byte[] ssid = new byte[0x20];
      content.readBytes(ssid);
      clientInfos.add(
          new ClientInfo(
              clientMac,
              nullTerminatedString(os),
              apMac,
              nullTerminatedString(ssid),
              nullTerminatedString(name)));
    }
  }

  @Override
  public ByteBuf toByteBuf() {
    return null;
  }

  @Override
  public String toString() {
    return "ClientInfoByVenueResp [clientInfos=" + clientInfos + "]";
  }

  public List<ClientInfo> getClientInfos() {
    return clientInfos;
  }
}
