package com.example.TestMqtt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("metrics.console")
public class MetricsConfig {

  public boolean getAll() {
    return all;
  }

  public void setAll(boolean all) {
    this.all = all;
  }

  public boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean getProbes() {
    return probes;
  }

  public void setProbes(boolean probes) {
    this.probes = probes;
  }

  private boolean all = false;
  private boolean enabled = true;
  private boolean probes = true;
}
