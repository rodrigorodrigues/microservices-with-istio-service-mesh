package com.github.todo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.vertx.core.http.HttpHeaders;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

public class WireMockQuarkusTestResource implements QuarkusTestResourceLifecycleManager {
	private static WireMockServer wireMockServer;

	@Override
	public Map<String, String> start() {
		wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
		wireMockServer.start();

		String json;
		int port = wireMockServer.port();
		try {
			json = String.join("", Files
				.readAllLines(new File("src/test/resources/__files/instancesByApplicationName.json").toPath()))
				.replaceAll("1111", String.valueOf(port));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		wireMockServer.stubFor(get(urlEqualTo("/eureka/apps/PERSON-SERVICE")).willReturn(WireMock.aResponse()
				.withHeader(HttpHeaders.CONTENT_TYPE.toString(), MediaType.APPLICATION_JSON)
				.withStatus(200)
				.withBody(json)));

		wireMockServer.stubFor(get(urlPathEqualTo("/api/people/default@admin.com")).willReturn(WireMock.aResponse()
				.withHeader(HttpHeaders.CONTENT_TYPE.toString(), MediaType.APPLICATION_JSON)
				.withStatus(200)
				.withBody("{\"name\":\"Test\"}")));

		System.setProperty("WIREMOCK_PORT", String.valueOf(port));
		return Collections.emptyMap();
	}

	@Override
	public void stop() {
		if (wireMockServer != null) {
			wireMockServer.shutdown();
		}
	}
}
