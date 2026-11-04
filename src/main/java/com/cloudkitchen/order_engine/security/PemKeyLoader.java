package com.cloudkitchen.order_engine.security;

import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Loads an RSA public key from a PEM resource (classpath: or file: path).
 * The key is expected in X.509 (SubjectPublicKeyInfo) format.
 */
public final class PemKeyLoader {

    private PemKeyLoader() {
    }

    public static PublicKey readPublicKey(Resource resource) {
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return factory.generatePublic(new X509EncodedKeySpec(der(resource)));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA public key from " + resource, e);
        }
    }

    private static byte[] der(Resource resource) throws Exception {
        String pem = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String base64 = pem
                .replaceAll("-----BEGIN [^-]+-----", "")
                .replaceAll("-----END [^-]+-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }
}
