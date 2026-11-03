package com.example.stay.common;

import com.example.stay.auth.config.AuthProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(AuthProperties.class)
public class AppConfig {
}
