package com.notification.consumer;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.notification.consumer.config.NotificationProperties;
import com.notification.consumer.config.TemplateProperties;

@SpringBootApplication
@EnableConfigurationProperties({NotificationProperties.class, TemplateProperties.class})
public class ConsumerApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(ConsumerApplication.class, args);
	}

}
