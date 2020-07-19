package com.github.microservices;

import java.io.File;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.context.support.GenericWebApplicationContext;

@Slf4j
public class InitPublicKeyConfiguration implements ApplicationContextInitializer<GenericWebApplicationContext> {

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
