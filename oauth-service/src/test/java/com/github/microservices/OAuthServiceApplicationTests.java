package com.github.microservices;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ContextConfiguration(initializers = {CertKeyConfigurationInitializer.class, OAuthServiceApplicationTests.UserMockConfiguration.class})
@AutoConfigureMockMvc
class OAuthServiceApplicationTests {

	static class UserMockConfiguration implements ApplicationContextInitializer<GenericApplicationContext> {
		@Override
		public void initialize(GenericApplicationContext applicationContext) {
			TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext,
					"com.github.users[0].username=admin",
					"com.github.users[0].password=admin",
					"com.github.users[0].scopes=admin");
		}
	}

	@Autowired
	MockMvc mockMvc;

	@Test
	void contextLoads() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/.well-known/jwks.json"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$..keys").isNotEmpty());
	}

}
