package com.github.microservices;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "logging.level.org.springframework.security=debug")
@ContextConfiguration(initializers = CertKeyConfigurationInitializer.class)
@AutoConfigureMockMvc
class OAuthCommonApplicationTests {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@Test
	void testJwks() throws Exception {
		mockMvc.perform(get("/.well-known/jwks.json"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$..keys").isNotEmpty());
	}

	@Test
	void testAuthentication() throws Exception {
		LinkedMultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("client_id", "admin");
		formData.add("username", "admin");
		formData.add("password", "test");
		formData.add("grant_type", "client_credentials");

		MockHttpServletResponse response = mockMvc.perform(post("/oauth/token")
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
				.params(formData)
				.with(httpBasic("admin", "admin")))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
				.andExpect(jsonPath("$.access_token", is(notNullValue())))
				.andExpect(jsonPath("$.token_type", is(notNullValue())))
				.andReturn()
				.getResponse();
		OAuth2AccessToken token = objectMapper.readValue(response
				.getContentAsString(), OAuth2AccessToken.class);

		assertThat(token).isNotNull();
		assertThat(token.getValue()).isNotEmpty();

		//TODO Fix this later
//		mockMvc.perform(get("/").header(HttpHeaders.AUTHORIZATION, token.getTokenType() + " " + token.getValue()))
//			.andExpect(status().isOk());
	}

}
