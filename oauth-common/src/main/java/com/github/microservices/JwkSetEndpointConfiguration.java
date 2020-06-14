package com.github.microservices;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerSecurityConfiguration;

@Configuration
@Order(1)
public class JwkSetEndpointConfiguration extends AuthorizationServerSecurityConfiguration {
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
				.requestMatchers()
				.mvcMatchers("/.well-known/jwks.json", "/actuator/**", "/login")
				.and()
				.authorizeRequests()
				.mvcMatchers("/.well-known/jwks.json", "/actuator/**", "/login").permitAll();
	}
}