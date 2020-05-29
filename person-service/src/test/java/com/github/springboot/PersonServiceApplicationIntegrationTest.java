package com.github.springboot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springboot.dto.PersonDto;
import com.github.springboot.model.Person;
import com.github.springboot.repository.PersonRepository;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = PersonServiceApplication.class,
		properties = {"configuration.swagger=false",
            "logging.level.com.github.springboot=debug"})
@ContextConfiguration(initializers = PersonServiceApplicationIntegrationTest.GenerateKeyPairInitializer.class)
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
public class PersonServiceApplicationIntegrationTest {
	@Autowired
	MockMvc mockMvc;

	@Autowired
    ObjectMapper objectMapper;

	@Autowired
	PersonRepository personRepository;

	@Autowired
    OAuth2ResourceServerProperties oAuth2ResourceServerProperties;

	@Autowired
	KeyPair keyPair;

	Person person;

	static class GenerateKeyPairInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

		@SneakyThrows
		@Override
		public void initialize(GenericApplicationContext applicationContext) {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
			kpg.initialize(2048);
			KeyPair kp = kpg.generateKeyPair();
			RSAPublicKey pub = (RSAPublicKey) kp.getPublic();
			Key pvt = kp.getPrivate();

			Base64.Encoder encoder = Base64.getEncoder();

			Path privateKeyFile = Files.createTempFile("privateKeyFile", ".key");
			Path publicKeyFile = Files.createTempFile("publicKeyFile", ".cert");

			Files.write(privateKeyFile,
					Arrays.asList("-----BEGIN PRIVATE KEY-----", encoder
							.encodeToString(pvt.getEncoded()), "-----END PRIVATE KEY-----"));
			log.info("Loaded private key: {}", privateKeyFile);

			Files.write(publicKeyFile,
					Arrays.asList("-----BEGIN PUBLIC KEY-----", encoder
							.encodeToString(pub.getEncoded()), "-----END PRIVATE KEY-----"));
			log.info("Loaded public key: {}", publicKeyFile);
			applicationContext.registerBean(RSAPublicKey.class, () -> pub);

			applicationContext.registerBean(KeyPair.class, () -> kp);
		}
	}

	Map<String, List<GrantedAuthority>> users = new HashMap<>();

    {
        users.put("default@admin.com", Collections.singletonList(new SimpleGrantedAuthority("admin")));
        users.put("anonymous@gmail.com", Collections.singletonList(new SimpleGrantedAuthority("people:read")));
        users.put("master@gmail.com", Arrays.asList(new SimpleGrantedAuthority("people:create"),
            new SimpleGrantedAuthority("people:read"),
            new SimpleGrantedAuthority("people:update")));
    }

    @BeforeEach
    public void setup() {
		RSAKey.Builder builder = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
				.keyUse(KeyUse.SIGNATURE)
				.algorithm(JWSAlgorithm.RS256)
				.keyID("test");
		JWKSet jwkSet = new JWKSet(builder.build());

		String jsonPublicKey = jwkSet.toJSONObject().toJSONString();
		log.debug("jsonPublicKey: {}", jsonPublicKey);
		stubFor(WireMock.get(urlPathEqualTo("/.well-known/jwks.json"))
				.willReturn(aResponse().withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).withBody(jsonPublicKey)));

        person = personRepository.save(Person.builder()
            .name("Person Master")
            .createdByUser("master@gmail.com")
            .build());
    }

    @AfterEach
    public void tearDown() {
        personRepository.delete(person);
    }

    @Test
	@DisplayName("Test - When Calling GET - /api/people should return filter list of people and response 200 - OK")
	public void shouldReturnListOfPeopleWhenCallApi() throws Exception {
		String authorizationHeader = authorizationHeader("master@gmail.com");

        mockMvc.perform(MockMvcRequestBuilders.get("/api/people")
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$..id", hasSize(1)));
	}

    @Test
    @DisplayName("Test - When Calling GET - /api/people should return list of people and response 200 - OK")
    public void shouldReturnListOfAllPeopleWhenCallApi() throws Exception {
        String authorizationHeader = authorizationHeader("default@admin.com");

        mockMvc.perform(get("/api/people")
            .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
            .andExpect(status().isOk())
			.andExpect(jsonPath("$..id", hasSize(4)));
    }

	@Test
    @DisplayName("Test - When Calling POST - /api/people should create a new person and response 201 - Created")
	public void shouldInsertNewCompanyWhenCallApi() throws Exception {
		String authorizationHeader = authorizationHeader("master@gmail.com");
		PersonDto person = createPerson();

		String json = mockMvc.perform(post("/api/people")
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.content(convertToJson(person)))
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/people/")))
				.andExpect(jsonPath("$..id").hasJsonPath())
				.andExpect(jsonPath("$.createdByUser").value("master@gmail.com"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(json).isNotBlank();
		PersonDto personDto = objectMapper.readValue(json, PersonDto.class);
		assertThat(personDto).isNotNull();
		assertThat(personDto.getId()).isNotEmpty();

		mockMvc.perform(delete("/api/people/{id}", personDto.getId())
            .header(HttpHeaders.AUTHORIZATION, authorizationHeader("default@admin.com")))
            .andExpect(status().is2xxSuccessful());
	}

    @Test
    @DisplayName("Test - When Calling POST - /api/people without mandatory field should response 400 - Bad Request")
	public void shouldResponseBadRequestWhenCallApiWithoutValidRequest() throws Exception {
		String authorizationHeader = authorizationHeader("default@admin.com");

		PersonDto personDto = createPerson();
		personDto.setName("");
		personDto.setLastModifiedDate(null);

		mockMvc.perform(post("/api/people")
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.content(convertToJson(personDto)))
				.andExpect(status().is4xxClientError());
	}

	@Test
    @DisplayName("Test - When Calling POST - /api/people without valid authorization should response 403 - Forbidden")
	public void shouldResponseForbiddenWhenCallApiWithoutRightPermission() throws Exception {
		String authorizationHeader = authorizationHeader("anonymous@gmail.com");

		PersonDto company = createPerson();

		mockMvc.perform(post("/api/people")
				.header(HttpHeaders.AUTHORIZATION, authorizationHeader)
				.contentType(MediaType.APPLICATION_JSON)
				.content(convertToJson(company)))
				.andExpect(status().isForbidden());
	}

	@SneakyThrows
    private String authorizationHeader(String user) {
        if (users.containsKey(user)) {
			JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
					.subject(user)
					.expirationTime(Date.from(ZonedDateTime.now().plusMinutes(1).toInstant()))
					.issueTime(new Date())
					.notBeforeTime(new Date())
					.claim("scope", users.get(user).stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()))
					.jwtID(UUID.randomUUID().toString())
					.issuer("jwt")
					.build();
			JWSSigner signer = new RSASSASigner(keyPair.getPrivate());
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("kid", "test");
			jsonObject.put("alg", JWSAlgorithm.RS256.getName());
			jsonObject.put("typ", "JWT");
			SignedJWT signedJWT = new SignedJWT(JWSHeader.parse(jsonObject), jwtClaimsSet);
			signedJWT.sign(signer);
			return "Bearer " + signedJWT.serialize();

		} else {
            return null;
        }
	}

	private PersonDto createPerson() {
		return PersonDto.builder()
			.name("Person Test")
			.lastModifiedDate(Instant.now())
			.build();
	}

	private String convertToJson(PersonDto personDto) throws JsonProcessingException {
        return objectMapper.writeValueAsString(personDto);
	}

}
