package com.cloudkitchen.order_engine.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * order-engine only verifies tokens. These tests confirm it accepts a token
 * signed by the matching RSA private key and rejects one signed by any other
 * key (i.e. it cannot be fooled and holds no minting capability).
 */
class JwtUtilTest {

    @Test
    void verifiesTokenSignedByMatchingPrivateKey() throws Exception {
        KeyPair kp = generateKeyPair();
        String token = sign(kp, "bob", "KITCHEN");

        JwtUtil jwtUtil = new JwtUtil(publicPem(kp));

        assertEquals("bob", jwtUtil.extractUsername(token));
        assertEquals("KITCHEN", jwtUtil.extractRole(token));
    }

    @Test
    void rejectsTokenSignedByDifferentKey() throws Exception {
        KeyPair signer = generateKeyPair();
        KeyPair unrelated = generateKeyPair();
        String token = sign(signer, "bob", "KITCHEN");

        JwtUtil jwtUtil = new JwtUtil(publicPem(unrelated));

        assertThrows(Exception.class, () -> jwtUtil.extractUsername(token));
    }

    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        return kpg.generateKeyPair();
    }

    private static String sign(KeyPair kp, String subject, String role) {
        return Jwts.builder()
                .setSubject(subject)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(kp.getPrivate(), SignatureAlgorithm.RS256)
                .compact();
    }

    private static ByteArrayResource publicPem(KeyPair kp) {
        String b64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                .encodeToString(kp.getPublic().getEncoded());
        String pem = "-----BEGIN PUBLIC KEY-----\n" + b64 + "\n-----END PUBLIC KEY-----\n";
        return new ByteArrayResource(pem.getBytes(StandardCharsets.UTF_8));
    }
}
