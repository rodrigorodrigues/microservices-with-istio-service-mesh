package com.github.microservices;

import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties(prefix = "com.github")
@Validated
public class UserCredentialsProperties {
	@Size(min = 1)
	private List<User> users;

	@Data
	public static class User {
		@NotBlank
		private String username;
		@NotBlank
		private String password;
		@Size(min = 1)
		private List<String> scopes;
	}
}
