package com.github.todo;

import javax.inject.Singleton;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.quarkus.jackson.ObjectMapperCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class JacksonCustomizer implements ObjectMapperCustomizer {
	private static final Logger log = LoggerFactory.getLogger(JacksonCustomizer.class);

	@Override
	public void customize(ObjectMapper objectMapper) {
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		objectMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY);
		log.info("Disabled WRITE_DATES_AS_TIMESTAMPS:registerModules: {}", objectMapper.getRegisteredModuleIds());
	}
}
