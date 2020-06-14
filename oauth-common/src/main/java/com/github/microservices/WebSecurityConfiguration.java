package com.github.microservices;

import java.util.stream.Collectors;

import lombok.AllArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@EnableWebSecurity
@Order(2)
@AllArgsConstructor
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {
	private final UserCredentialsProperties userCredentialsProperties;
	private final PasswordEncoder passwordEncoder;

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(userDetailsService())
				.passwordEncoder(passwordEncoder);
	}

	@Bean
	@Override
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManager();
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
			.csrf()
			.disable()
			.authorizeRequests()
			.antMatchers("/oauth/token", "/error", "/favicon.ico").permitAll()
			.anyRequest()
			.authenticated()
			.and()
			.anonymous()
			.disable();
	}

	@Bean
	@Override
	protected UserDetailsService userDetailsService() {
		return new InMemoryUserDetailsManager(userCredentialsProperties.getUsers()
				.stream()
				.map(u -> User.withUsername(u.getUsername())
						.passwordEncoder(s -> passwordEncoder.encode(u.getPassword()))
						.roles(u.getScopes().toArray(new String[] {}))
						.build())
				.collect(Collectors.toList()));
	}
}