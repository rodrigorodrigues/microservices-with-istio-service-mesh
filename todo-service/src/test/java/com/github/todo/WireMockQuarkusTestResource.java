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

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;

public class WireMockQuarkusTestResource implements QuarkusTestResourceLifecycleManager {
	private static WireMockServer wireMockServer;

	@Override
	public Map<String, String> start() {
		wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
		wireMockServer.start();

		String json;
		try {
			json = String.join("", Files
				.readAllLines(new File("src/test/resources/__files/person.json").toPath()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		wireMockServer.stubFor(get(anyUrl()).willReturn(WireMock.aResponse()
				.withHeader(HttpHeaders.CONTENT_TYPE.toString(), MediaType.APPLICATION_JSON)
				.withStatus(200)
				.withBody(json)));

		int port = wireMockServer.port();
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
