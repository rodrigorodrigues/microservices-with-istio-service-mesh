package com.github.microservices;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class IstioOAuthServiceApplication {
	public static void main(String[] args) {
		new SpringApplicationBuilder(IstioOAuthServiceApplication.class)
				.initializers(new CertKeyConfigurationInitializer())
				.run(args);
	}

}
