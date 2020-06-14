package com.github.microservices;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class IstioAuthServiceApplication {
	public static void main(String[] args) {
		new SpringApplicationBuilder(IstioAuthServiceApplication.class)
				.initializers(new CertKeyConfigurationInitializer())
				.run(args);
	}

}
