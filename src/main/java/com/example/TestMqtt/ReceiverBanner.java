package com.example.TestMqtt;

import org.springframework.boot.Banner;
import org.springframework.core.env.Environment;

import java.io.PrintStream;

public class ReceiverBanner implements Banner {

  public String portParam;
  private String name;

  public ReceiverBanner(String port, String name) {
    this.portParam = port;
    this.name = name;
  }

  @Override
  public void printBanner(Environment environment, Class<?> sourceClass, PrintStream out) {
    String port = environment.getProperty(portParam);

    out.println("************************");
    out.println("*");
    out.println("* Starting " + name + " on port " + port);
    out.println("*");
    out.println("************************");
  }
}
