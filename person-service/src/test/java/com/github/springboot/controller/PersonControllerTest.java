package com.github.springboot.controller;

import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springboot.config.SpringSecurityConfiguration;
import com.github.springboot.dto.PersonDto;
import com.github.springboot.service.PersonService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(properties = {
        "configuration.initialLoad=false",
        "configuration.mongo=false"},
controllers = PersonController.class, excludeAutoConfiguration = MongoAutoConfiguration.class)
@Import({SpringSecurityConfiguration.class, ErrorMvcAutoConfiguration.class})
public class PersonControllerTest {

    @Autowired
    MockMvc client;

    @MockBean
    PersonService personService;

    @MockBean
    RSAPublicKey publicKey;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("Test - When Calling GET - /api/people without valid authorization the response should be 403 - Forbidden")
    @WithMockUser(roles = "INVALID_ROLE")
    public void whenCallFindAllShouldReturnForbiddenWhenDoesNotHavePermission() throws Exception {
        client.perform(get("/api/people")
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/people without authorization the response should be 401 - Unauthorized")
    public void whenCallFindAllShouldReturnUnauthorizedWhenDoesNotHavePermission() throws Exception {
        client.perform(get("/api/people"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/people with admin role the response should be a list of Companies - 200 - OK")
    @WithMockUser(authorities = "SCOPE_admin")
    public void whenCallFindAllShouldReturnListOfCompanies() throws Exception {
        PersonDto personDto = new PersonDto();
        personDto.setId("100");
        PersonDto personDto1 = new PersonDto();
        personDto1.setId("200");
        when(personService.findAll(any())).thenReturn(Arrays.asList(personDto, personDto1));

        ParameterizedTypeReference<ServerSentEvent<PersonDto>> type = new ParameterizedTypeReference<ServerSentEvent<PersonDto>>() {};

        client.perform(get("/api/people")
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$..id", hasSize(2)));
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/people with SCOPE_people:read role the response should be filtered - 200 - OK")
    @WithMockUser(authorities = "SCOPE_people:read", username = "me")
    public void whenCallShouldFilterListOfCompanies() throws Exception {
        PersonDto personDto = new PersonDto();
        personDto.setId("100");
        personDto.setCreatedByUser("me");
        when(personService.findPeopleByCreatedUser(anyString(), any())).thenReturn(Arrays.asList(personDto));

        ParameterizedTypeReference<ServerSentEvent<PersonDto>> type = new ParameterizedTypeReference<ServerSentEvent<PersonDto>>() {};

        client.perform(get("/api/people")
            .header(HttpHeaders.AUTHORIZATION, "MOCK JWT"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$..id", hasSize(1)));
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/people/{id} with valid authorization the response should be person - 200 - OK")
    @WithMockUser(authorities = "SCOPE_people:read", username = "me")
    public void whenCallFindByIdShouldReturnPerson() throws Exception {
        PersonDto personDto = new PersonDto();
        personDto.setId("100");
        personDto.setCreatedByUser("me");
        when(personService.findById(anyString())).thenReturn(Optional.of(personDto));

        client.perform(get("/api/people/{id}", 100)
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", equalTo("100")));
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/people/{id} with different user should response 403 - Forbidden")
    @WithMockUser(authorities = "SCOPE_people:read", username = "test")
    public void whenCallFindByIdShouldResponseForbidden() throws Exception {
        PersonDto personDto = new PersonDto();
        personDto.setId("100");
        personDto.setCreatedByUser("test1");
        when(personService.findById(anyString())).thenReturn(Optional.of(personDto));

        client.perform(get("/api/people/{id}", 100)
            .header(HttpHeaders.AUTHORIZATION, "MOCK JWT"))
            .andExpect(status().isForbidden());
//            .andExpect(jsonPath("$.message", containsString("User(test) does not have access to this resource")));
    }

    @Test
    @DisplayName("Test - When Calling POST - /api/people with valid authorization the response should be a person - 201 - Created")
    @WithMockUser(authorities = "SCOPE_people:create")
    public void whenCallCreateShouldSavePerson() throws Exception {
        PersonDto personDto = createPersonDto();
        when(personService.save(any(PersonDto.class))).thenReturn(Optional.of(personDto));

        client.perform(post("/api/people")
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertToJson(personDto)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$..id", contains(personDto.getId())));
    }

    @Test
    @DisplayName("Test - When Calling PUT - /api/people/{id} with valid authorization the response should be a person - 200 - OK")
    @WithMockUser(authorities = "SCOPE_people:update")
    public void whenCallUpdateShouldUpdatePerson() throws Exception {
        PersonDto personDto = createPersonDto();
        personDto.setId(UUID.randomUUID().toString());
        personDto.setName("New Name");
        when(personService.findById(anyString())).thenReturn(Optional.of(personDto));
        when(personService.save(any(PersonDto.class))).thenReturn(Optional.of(personDto));

        client.perform(put("/api/people/{id}", personDto.getId())
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertToJson(personDto)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", equalTo(personDto.getId())))
                .andExpect(jsonPath("$.name", equalTo(personDto.getName())));
    }

    @Test
    @DisplayName("Test - When Calling PUT - /api/people/{id} with invalid id the response should be 404 - Not Found")
    @WithMockUser(authorities = "SCOPE_people:update")
    public void whenCallUpdateShouldResponseNotFound() throws Exception {
        PersonDto personDto = createPersonDto();
        personDto.setId("999");
        when(personService.findById(anyString())).thenReturn(Optional.empty());

        client.perform(put("/api/people/{id}", personDto.getId())
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertToJson(personDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Test - When Calling DELETE - /api/people/{id} with valid authorization the response should be 200 - OK")
    @WithMockUser(authorities = "SCOPE_people:delete", username = "mock")
    public void whenCallDeleteShouldDeleteById() throws Exception {
        PersonDto personDto = new PersonDto();
        personDto.setId("12345");
        personDto.setCreatedByUser("mock");
        when(personService.findById(anyString())).thenReturn(Optional.of(personDto));

        client.perform(delete("/api/people/{id}", personDto.getId())
                .header(HttpHeaders.AUTHORIZATION, "MOCK JWT"))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DisplayName("Test - When Calling DELETE - /api/people/{id} with different user  the response should be 403 - Forbidden")
    @WithMockUser(authorities = "SCOPE_people:delete", username = "test")
    public void whenCallDeleteWithDifferentUSerShouldResponseForbidden() throws Exception {
        PersonDto personDto = new PersonDto();
        personDto.setId("12345");
        personDto.setCreatedByUser("mock");
        when(personService.findById(anyString())).thenReturn(Optional.of(personDto));

        client.perform(delete("/api/people/{id}", personDto.getId())
            .header(HttpHeaders.AUTHORIZATION, "MOCK JWT"))
            .andExpect(status().isForbidden());
//            .andExpect(jsonPath("$.message", containsString("User(test) does not have access to delete this resource")));
    }

    @Test
    @DisplayName("Test - When Calling DELETE - /api/people/{id} with id that does not exist should response 404 - Not Found")
    @WithMockUser(authorities = "SCOPE_people:delete")
    public void whenCallDeleteShouldResponseNotFound() throws Exception {
        when(personService.findById(anyString())).thenReturn(Optional.empty());

        client.perform(delete("/api/people/{id}", "12345")
            .header(HttpHeaders.AUTHORIZATION, "MOCK JWT"))
            .andExpect(status().isNotFound());

        verify(personService, never()).deleteById(anyString());
    }

    private String convertToJson(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

    private PersonDto createPersonDto() {
        return PersonDto.builder()
                .id(UUID.randomUUID().toString())
                .name("Test")
                .lastModifiedDate(Instant.now())
                .build();
    }
}
