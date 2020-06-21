package com.github.microservices

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.jayway.jsonpath.JsonPath
import com.toomuchcoding.jsonassert.JsonAssertion.assertThatJson
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
class ZuulServerApplicationTests(@Autowired val restTemplate: TestRestTemplate) {

	@Test
	fun testGetAllTodosWithParameter() {
		stubFor(get(anyUrl())
				.willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody("[{\"name\":\"Test\", \"id\": 1002},"
								+ "{\"name\":\"Test 2\", \"id\": 1003}]")))

		val json = restTemplate.getForObject("/api/todos?pageSize=10", List::class.java)
		val documentContext = JsonPath.parse(json)
		assertThatJson(documentContext).array().hasSize(2)
		assertThatJson(documentContext).array("id").contains(listOf(1002, 1003))

		verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/todosMock")).withQueryParam("pageSize", equalTo("10")))
	}

	@Test
	fun testGetAllTodos() {
		stubFor(get(anyUrl())
				.willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody("[{\"name\":\"Test\", \"id\": 1002},"
								+ "{\"name\":\"Test 2\", \"id\": 1003}]")))

		val json = restTemplate.getForObject("/api/todos", List::class.java)
		val documentContext = JsonPath.parse(json)
		assertThatJson(documentContext).array().hasSize(2)
		assertThatJson(documentContext).array("id").contains(listOf(1002, 1003))

		verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/todosMock")))
	}

	@Test
	fun testGetTodoById() {
		stubFor(get(anyUrl())
				.willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody("{\"name\":\"Test\", \"id\": 1002}")))

		val json = restTemplate.getForObject("/api/todos/1002", String::class.java)
		assertThatJson(json).field("name").isEqualTo("Test")
		assertThatJson(json).field("id").isEqualTo(1002)

		verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/todosMock/1002")))
	}

	@Test
	fun testDeleteTodoById() {
		stubFor(delete(anyUrl())
				.willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withStatus(204)))

		val httpHeaders = HttpHeaders()
		val response = restTemplate.exchange("/api/todos/{id}", HttpMethod.DELETE, HttpEntity(null, httpHeaders), String::class.java, 1002)

		assert(response.statusCode == HttpStatus.NO_CONTENT)

		verify(exactly(1), deleteRequestedFor(urlPathEqualTo("/api/todosMock/1002")))
	}

	@Test
	fun testCreateTodoById() {
		stubFor(post(anyUrl())
				.willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody("{\"name\":\"Test\", \"id\": 1002}")))

		val httpHeaders = HttpHeaders()
		httpHeaders.contentType = APPLICATION_JSON
		httpHeaders.setBasicAuth("test", "test")

		val json = restTemplate.exchange("/api/todos", HttpMethod.POST, HttpEntity("{\"name\":\"Test\"}", httpHeaders), String::class.java).body
		assertThatJson(json).field("name").isEqualTo("Test")
		assertThatJson(json).field("id").isEqualTo(1002)

		verify(exactly(1), postRequestedFor(urlPathEqualTo("/api/todosMock")))
	}

	@Test
	fun testUpdateTodoById() {
		stubFor(put(anyUrl())
				.willReturn(aResponse().withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody("{\"name\":\"Testing\", \"id\": 1002}")))

		val httpHeaders = HttpHeaders()
		httpHeaders.contentType = APPLICATION_JSON

		val json = restTemplate.exchange("/api/todos/1002", HttpMethod.PUT, HttpEntity("{\"name\":\"Testing\"}", httpHeaders), String::class.java).body
		assertThatJson(json).field("name").isEqualTo("Testing")
		assertThatJson(json).field("id").isEqualTo(1002)

		verify(exactly(1), putRequestedFor(urlPathEqualTo("/api/todosMock/1002")))
	}

	@AfterEach
	fun tearDown() = reset()

}
