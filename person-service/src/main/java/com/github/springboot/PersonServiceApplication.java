package com.github.springboot;

import java.io.File;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.github.springboot.model.Person;
import com.github.springboot.repository.PersonRepository;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.context.support.GenericWebApplicationContext;

@Slf4j
@SpringBootApplication
@EnableCircuitBreaker
public class PersonServiceApplication {
    public static void main(String[] args) {
        new SpringApplicationBuilder(PersonServiceApplication.class)
            .initializers(new InitPublicKeyConfiguration())
            .run(args);
    }

    @Slf4j
    static class InitPublicKeyConfiguration implements ApplicationContextInitializer<GenericWebApplicationContext> {

        @SneakyThrows
        @Override
        public void initialize(GenericWebApplicationContext applicationContext) {
            ConfigurableEnvironment environment = applicationContext.getEnvironment();
            String publicKeyProperty = environment.getProperty("cert.publicKey");

            File publicKeyFile = (StringUtils.isNotBlank(publicKeyProperty) ? new File(publicKeyProperty) :
                    new File(System.getProperty("java.io.tmpdir"), "authPublicKey.pem"));

            applicationContext.registerBean(RSAPublicKey.class, () -> (RSAPublicKey) readPublicKey(publicKeyFile));
        }

        private PublicKey readPublicKey(File publicKeyFile) {
            try {
                byte[] encodedBytes;
                String publicKeyContent = new String(Files.readAllBytes(publicKeyFile.toPath()));
                encodedBytes = Base64.getDecoder().decode(removeBeginEnd(publicKeyContent));
                X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(encodedBytes);
                KeyFactory kf = KeyFactory.getInstance("RSA");
                return kf.generatePublic(pubSpec);
            } catch (Exception e) {
                log.error("error on method readPublicKey", e);
                throw new RuntimeException(e);
            }
        }

        private String removeBeginEnd(String pem) {
            pem = pem.replaceAll("-----BEGIN (.*)-----", "");
            pem = pem.replaceAll("-----END (.*)----", "");
            pem = pem.replaceAll("\r\n", "");
            pem = pem.replaceAll("\n", "");
            return pem.trim();
        }
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
