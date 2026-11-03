package com.example.stay.common;

import com.example.stay.common.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/** common 은 feature(auth 등)를 import 하지 않는다. feature 설정은 각 feature 의 Config 에서 등록. */
@Configuration
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(SecurityProperties.class)
public class AppConfig {
}
