package com.github.microservices;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class AuthCommonMockedApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder(AuthCommonMockedApplication.class)
				.initializers(new CertKeyConfigurationInitializer())
				.run(args);
	}

	@RestController
	@RequestMapping("/")
	class MockController {
		@GetMapping
		public String hello(@AuthenticationPrincipal OAuth2Authentication auth2Authentication) {
			return String.format("Hello %s", auth2Authentication.getName());
		}
	}
}
