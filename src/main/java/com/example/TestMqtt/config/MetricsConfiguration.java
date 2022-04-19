package com.example.TestMqtt.config;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Slf4jReporter.Builder;
import com.codahale.metrics.Slf4jReporter.LoggingLevel;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.dropwizard.DropwizardConfig;
import io.micrometer.core.instrument.dropwizard.DropwizardMeterRegistry;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

@Configuration
public class MetricsConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(MetricsConfiguration.class);

//  @Bean
//  MetricRegistry dropWizardMetricRegistry() {
//    return new MetricRegistry();
//  }


  MetricRegistry metricRegistry = new MetricRegistry();
  @Autowired
  MetricsConfig config;

  Slf4jReporter reporter;

  @Bean
  @ConditionalOnProperty(value = "metrics.console.enabled", havingValue = "true")
  public MeterRegistry consoleLoggingRegistry() {

    Marker metricMarker = MarkerFactory.getMarker("METRICS");
    Builder reporterBuilder =
        Slf4jReporter.forRegistry(metricRegistry)
            .outputTo(logger)
            .markWith(metricMarker)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .withLoggingLevel(LoggingLevel.INFO);

    if (!config.getAll()) {
      reporterBuilder =
          reporterBuilder.filter(
              (name, metric) -> {
                if (config.getProbes() && name.startsWith("probes.")) {
                  return true;
                }
                return name.startsWith("wlc-")
                    || name.startsWith("mqtt-")
                    || name.startsWith("ks-")
                    || name.startsWith("aruba-")
                    || name.startsWith("ruckus-")
                    || name.startsWith("generic-events-sent");
              });
    }

    reporter = reporterBuilder.build();
    reporter.start(60, TimeUnit.SECONDS);

    DropwizardConfig consoleConfig =
        new DropwizardConfig() {
          @Override
          public String prefix() {
            return "console";
          }

          @Override
          public String get(String key) {
            return null;
          }
        };

    MeterRegistry registry =
        new DropwizardMeterRegistry(
            consoleConfig, metricRegistry, HierarchicalNameMapper.DEFAULT, Clock.SYSTEM) {
          @Override
          protected Double nullGaugeValue() {
            return null;
          }
        };
    Metrics.addRegistry(registry);
    return registry;
  }

  @PreDestroy
  public void shutdown() {
    reporter.close();
  }
}
