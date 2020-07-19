package com.github.microservices;

import java.security.KeyPair;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.config.annotation.builders.InMemoryClientDetailsServiceBuilder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

@EnableAuthorizationServer
@Configuration
public class AuthorizationServerConfiguration extends AuthorizationServerConfigurerAdapter {

	private final AuthenticationManager authenticationManager;
	private final KeyPair keyPair;
	private final PasswordEncoder passwordEncoder;
	private final UserCredentialsProperties userCredentialsProperties;

	public AuthorizationServerConfiguration(AuthenticationConfiguration authenticationConfiguration,
			KeyPair keyPair, PasswordEncoder passwordEncoder, UserCredentialsProperties userCredentialsProperties) throws Exception {
		this.authenticationManager = authenticationConfiguration.getAuthenticationManager();
		this.keyPair = keyPair;
		this.passwordEncoder = passwordEncoder;
		this.userCredentialsProperties = userCredentialsProperties;
	}

	@Override
	public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
		InMemoryClientDetailsServiceBuilder inMemoryBuilder = clients.inMemory();
		for(UserCredentialsProperties.User user : userCredentialsProperties.getUsers()) {
			inMemoryBuilder
					.withClient(user.getUsername())
					.secret(passwordEncoder.encode(user.getPassword()))
					.scopes(user.getScopes().toArray(new String[] {}))
					.autoApprove(true)
					.authorizedGrantTypes("authorization_code", "client_credentials", "password", "refresh_token", "implicit");
		}
	}

	@Override
	public void configure(AuthorizationServerSecurityConfigurer security) {
		security
				.tokenKeyAccess("permitAll()")
				.checkTokenAccess("isAuthenticated()");
	}

	@Override
	public void configure(AuthorizationServerEndpointsConfigurer endpoints) {
		// @formatter:off
		endpoints
				.authenticationManager(this.authenticationManager)
				.accessTokenConverter(accessTokenConverter())
				.tokenStore(tokenStore());
		// @formatter:on
	}

	@Bean
	public TokenStore tokenStore() {
		return new JwtTokenStore(accessTokenConverter());
	}

	@Bean
	public JwtAccessTokenConverter accessTokenConverter() {
		JwtAccessTokenConverter converter = new JwtAccessTokenConverter() {
			@Override
			public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
				Map<String, Object> additionalInfo = new HashMap<>();
				additionalInfo.put("name", authentication.getName());
				additionalInfo.put("sub", authentication.getName());
				long currentTime = new Date().getTime() / 1000;
				additionalInfo.put("iat", currentTime);
				additionalInfo.put("nbf", currentTime);
				additionalInfo.put("iss", "jwt");
				additionalInfo.put("aud", "jwt");
				((DefaultOAuth2AccessToken) accessToken).setAdditionalInformation(additionalInfo);
				return super.enhance(accessToken, authentication);
			}
		};
		converter.setKeyPair(this.keyPair);
		return converter;
	}
}
