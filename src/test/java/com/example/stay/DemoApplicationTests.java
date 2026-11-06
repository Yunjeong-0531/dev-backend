package com.example.stay;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class DemoApplicationTests {

	@DynamicPropertySource
	static void overrideProperties(DynamicPropertyRegistry registry) {
		registry.add("app.security.jwt.secret",
				() -> "test-only-secret-key-must-be-at-least-32-bytes!!");
	}

	@Test
	void contextLoads() {
	}

}
