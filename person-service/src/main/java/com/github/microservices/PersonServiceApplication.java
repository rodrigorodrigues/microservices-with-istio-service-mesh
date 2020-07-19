package com.github.microservices;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.github.microservices.model.Person;
import com.github.microservices.repository.PersonRepository;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Slf4j
@SpringBootApplication
@EnableCircuitBreaker
public class PersonServiceApplication {
    public static void main(String[] args) {
        new SpringApplicationBuilder(PersonServiceApplication.class)
            .initializers(new InitPublicKeyConfiguration())
            .run(args);
    }


    @ConditionalOnProperty(prefix = "configuration", name = "mongo", havingValue = "true", matchIfMissing = true)
    @Configuration
    @EnableJpaRepositories(basePackageClasses = PersonRepository.class)
    static class MongoConfiguration {
    }

    @Bean
    Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return jacksonObjectMapperBuilder -> {
            jacksonObjectMapperBuilder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            jacksonObjectMapperBuilder.serializationInclusion(JsonInclude.Include.NON_EMPTY);
            jacksonObjectMapperBuilder.dateFormat(new StdDateFormat());
        };
    }

    @ConditionalOnProperty(prefix = "configuration", name = "initialLoad", havingValue = "true", matchIfMissing = true)
    @Bean
    CommandLineRunner runner(PersonRepository personRepository) {
        return args -> {
            if (personRepository.count() == 0) {
                Iterable<Person> people = personRepository.saveAll(Arrays.asList(
                    new Person("Admin", "default@admin.com"),
                    new Person("Test", "default@admin.com"),
                    new Person("Another Person", "default@admin.com")));
                log.debug("Saved Default People:size: {}", people);
            }
        };
    }

    @Primary
    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }
}
