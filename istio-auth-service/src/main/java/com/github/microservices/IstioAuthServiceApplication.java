package com.github.microservices;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.security.oauth2.provider.endpoint.FrameworkEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@SpringBootApplication
public class IstioAuthServiceApplication {
	private static Logger log = LoggerFactory.getLogger(IstioAuthServiceApplication.class);

	public static void main(String[] args) {
		new SpringApplicationBuilder(IstioAuthServiceApplication.class)
				.initializers(new CertKeyConfigurationInitializer())
				.run(args);
	}

	static class CertKeyConfigurationInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

		@Override
		public void initialize(GenericApplicationContext applicationContext) {
			ConfigurableEnvironment environment = applicationContext.getEnvironment();
			String privateKeyProperty = environment.getProperty("cert.privateKey");
			String publicKeyProperty = environment.getProperty("cert.publicKey");

			File privateKeyFile = (StringUtils.isNotBlank(privateKeyProperty) ? new File(privateKeyProperty) :
					new File(System.getProperty("java.io.tmpdir"), "authPrivateKey.pem"));

			File publicKeyFile = (StringUtils.isNotBlank(publicKeyProperty) ? new File(publicKeyProperty) :
					new File(System.getProperty("java.io.tmpdir"), "authPublicKey.pem"));

			if (!privateKeyFile.exists() || privateKeyFile.length() == 0) {
				generatePrivateAndPublicKey(applicationContext, privateKeyFile, publicKeyFile);
			} else {
				KeyPair keyPair = readPrivateKey(privateKeyFile, publicKeyFile);

				applicationContext.registerBean(KeyPair.class, () -> keyPair);
				applicationContext.registerBean(RSAPublicKey.class, () -> (RSAPublicKey) keyPair.getPublic());

				log.info("loaded cert: {} ", keyPair);
			}
		}

		private KeyPair readPrivateKey(File privateKeyFile, File publicKeyFile) {
			try {
				String privateKeyContent = new String(Files.readAllBytes(privateKeyFile.toPath()));
				byte[] encodedBytes = Base64.getDecoder().decode(removeBeginEnd(privateKeyContent));

				PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedBytes);
				KeyFactory kf = KeyFactory.getInstance("RSA");
				PrivateKey privateKey = kf.generatePrivate(keySpec);

				PublicKey publicKey = readPublicKey(publicKeyFile);
				return new KeyPair(publicKey, privateKey);
			} catch (Exception e) {
				log.error("error on method readPrivateKey", e);
				throw new RuntimeException(e);
			}
		}

		private PublicKey readPublicKey(File publicKeyFile) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
			byte[] encodedBytes;
			String publicKeyContent = new String(Files.readAllBytes(publicKeyFile.toPath()));
			encodedBytes = Base64.getDecoder().decode(removeBeginEnd(publicKeyContent));
			X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(encodedBytes);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			return kf.generatePublic(pubSpec);
		}

		private void generatePrivateAndPublicKey(GenericApplicationContext applicationContext, File privateKeyFile, File publicKeyFile) {
			try {
				KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
				kpg.initialize(2048);
				KeyPair kp = kpg.generateKeyPair();
				RSAPublicKey pub = (RSAPublicKey) kp.getPublic();
				Key pvt = kp.getPrivate();

				Base64.Encoder encoder = Base64.getEncoder();

				Files.write(privateKeyFile.toPath(),
						Arrays.asList("-----BEGIN PRIVATE KEY-----", encoder
								.encodeToString(pvt.getEncoded()), "-----END PRIVATE KEY-----"));
				log.info("Loaded private key: {}", privateKeyFile.toPath());

				if (!publicKeyFile.exists() || publicKeyFile.length() == 0) {
					Files.write(publicKeyFile.toPath(),
							Arrays.asList("-----BEGIN PUBLIC KEY-----", encoder
									.encodeToString(pub.getEncoded()), "-----END PRIVATE KEY-----"));
					log.info("Loaded public key: {}", privateKeyFile.toPath());
					applicationContext.registerBean(RSAPublicKey.class, () -> pub);
				}
				else {
					RSAPublicKey publicKey = (RSAPublicKey) readPublicKey(publicKeyFile);
					applicationContext.registerBean(RSAPublicKey.class, () -> publicKey);
				}

				applicationContext.registerBean(KeyPair.class, () -> kp);
			} catch (Exception e) {
				log.error("error on method generatePrivateAndPublicKey", e);
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

	@FrameworkEndpoint
	class JwkSetEndpoint {
		KeyPair keyPair;

		public JwkSetEndpoint(KeyPair keyPair) {
			this.keyPair = keyPair;
		}

		@GetMapping("/.well-known/jwks.json")
		@ResponseBody
		public Map<String, Object> getKey() {
			RSAPublicKey publicKey = (RSAPublicKey) this.keyPair.getPublic();
			RSAKey key = new RSAKey.Builder(publicKey).build();
			return new JWKSet(key).toJSONObject();
		}
	}
}
