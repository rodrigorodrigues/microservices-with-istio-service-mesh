package com.github.microservices

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.netflix.zuul.EnableZuulProxy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpMethod.*
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import org.springframework.web.servlet.function.ServerResponse.status
import org.springframework.web.servlet.function.router


@SpringBootApplication
@EnableZuulProxy
class ZuulServerApplication
	fun main(args: Array<String>) {
		runApplication<ZuulServerApplication>(*args)
	}

@Configuration
class RestTemplateConfiguration {
	@Bean
	fun restTemplate() : RestTemplate {
		val restTemplate = RestTemplate()
		restTemplate.requestFactory = HttpComponentsClientHttpRequestFactory()
		return restTemplate
	}
}

@Configuration
class TodoRouters(private val handler: TodoHandler) {
	@Bean
	fun router() = router {
		"/api/todos".nest {
			GET("/", handler::findAll)
			GET("/getTotalCategory", handler::getTotalCategory)
			GET("/{id}", handler::findById)
			contentType(APPLICATION_JSON).nest {
				POST("/", handler::create)
				PUT("/{id}", handler::update)
			}
			DELETE("/{id}", handler::deleteById)
		}
		GET("/api/dashboard/totalCategory", handler::getDashboard)
	}
}

@Component
class TodoHandler(private val restTemplate: RestTemplate,
				  @Value("\${todoUrl:http://localhost:8082/api/todos}") val url: String,
				  @Value("\${dashboardUrl:http://localhost:8084/api/dashboard/totalCategory}") val dashboardUrl: String) {
	fun create(req: ServerRequest) : ServerResponse {
		val body = req.body(String::class.java)

		val httpHeaders = generateHttpHeaders(req)

		return processRequest(url, POST, HttpEntity(body, httpHeaders), String::class.java)
	}

	fun update(req: ServerRequest) : ServerResponse {
		val body = req.body(String::class.java)

		val httpHeaders = generateHttpHeaders(req)

		val id = req.pathVariable("id")

		return processRequest("$url/$id", PUT, HttpEntity(body, httpHeaders), String::class.java)
	}

	fun deleteById(req: ServerRequest) : ServerResponse {
		val id = req.pathVariable("id")

		val httpHeaders = generateHttpHeaders(req)

		return processRequest("$url/$id", DELETE, HttpEntity(null, httpHeaders), String::class.java)
	}

	fun findById(req: ServerRequest) : ServerResponse {
		val id = req.pathVariable("id")

		val httpHeaders = generateHttpHeaders(req)

		return processRequest("$url/$id", GET, HttpEntity(null, httpHeaders), String::class.java)
	}

	fun getDashboard(req: ServerRequest) : ServerResponse {
		val httpHeaders = generateHttpHeaders(req)

		val queryString = req.servletRequest().queryString
		val url = if (queryString != null) "$dashboardUrl?$queryString" else "$dashboardUrl"
		return processRequest(url, GET, HttpEntity(null, httpHeaders), String::class.java)
	}

	fun getTotalCategory(req: ServerRequest) : ServerResponse {
		val httpHeaders = generateHttpHeaders(req)

		val queryString = req.servletRequest().queryString
		val url = if (queryString != null) "$url/getTotalCategory?$queryString" else "$url/getTotalCategory"
		return processRequest(url, GET, HttpEntity(null, httpHeaders), String::class.java)
	}

	fun findAll(req: ServerRequest) : ServerResponse {
		val httpHeaders = generateHttpHeaders(req)

		val queryString = req.servletRequest().queryString
		val respParamType = object: ParameterizedTypeReference<List<Any>>(){}
		val url = if (queryString != null) "$url?$queryString" else url
		return processRequest(url, GET, HttpEntity(null, httpHeaders), respParamType = respParamType)
	}

	private fun processRequest(url: String, httpMethod: HttpMethod,
							   httpEntity: HttpEntity<*>, respType: Class<*>? = null,
							   respParamType: ParameterizedTypeReference<*>? = null) : ServerResponse {
		return try {
			val exchange = if (respParamType != null) {
				restTemplate.exchange(url, httpMethod, httpEntity, respParamType)
			} else {
				restTemplate.exchange(url, httpMethod, httpEntity, respType!!)
			}
			status(exchange.statusCode)
					.headers {
						exchange.headers.forEach { k, v -> it[k] = v }
					}
					.body(exchange.body?:"")
		} catch (e: RestClientResponseException) {
			status(e.rawStatusCode)
					.headers {
						e.responseHeaders?.forEach { k, v -> it[k] = v }
					}
					.body(e.responseBodyAsString)
		}
	}

	private fun generateHttpHeaders(req: ServerRequest): HttpHeaders {
		val httpHeaders = HttpHeaders()
		httpHeaders.putAll(req.headers().asHttpHeaders())
		return httpHeaders
	}
}
