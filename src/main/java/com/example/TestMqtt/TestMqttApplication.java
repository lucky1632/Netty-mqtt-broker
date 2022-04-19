package com.example.TestMqtt;

import com.codahale.metrics.MetricRegistry;
import com.example.TestMqtt.broker.MqttBroker;
import com.example.TestMqtt.config.ConfigProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties({ConfigProperties.class})
public class TestMqttApplication {

	public static void main(String[] args) {
		SpringApplication.run(TestMqttApplication.class, args);
	}

//	@Bean
//	MqttBroker mqttBroker(ConfigProperties properties){
//		System.out.println("Mqtt Bean is created "+ properties.getDeviceConfig().getPort());
//		return new MqttBroker(properties.getDeviceConfig(), new MetricRegistry());
//	}

}
