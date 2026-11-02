package com.smarttourplanner.admin;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/** Signs tokens the way Cloudflare Access does, with a key made for the tests. */
final class AccessTokens {

    static final String ISSUER = "https://smarttour-test.cloudflareaccess.com";
    static final String AUDIENCE = "test-admin-app";

    private final RSAKey key = generate();

    /** A verifier that trusts only this key, like the real one trusts only Cloudflare's. */
    AccessTokenVerifier verifier() {
        return new AccessTokenVerifier(new ImmutableJWKSet<>(new JWKSet(key.toPublicJWK())), ISSUER, AUDIENCE);
    }

    String forEmail(String email) {
        return sign(claims(email).build());
    }

    JWTClaimsSet.Builder claims(String email) {
        Instant now = Instant.now();
        return new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .audience(AUDIENCE)
                .subject("user-1")
                .claim("email", email)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(Duration.ofHours(1))));
    }

    String sign(JWTClaimsSet claims) {
        return sign(claims, key);
    }

    static String sign(JWTClaimsSet claims, RSAKey signingKey) {
        try {
            SignedJWT token = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(), claims);
            token.sign(new RSASSASigner(signingKey));
            return token.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    static RSAKey generate() {
        try {
            return new RSAKeyGenerator(2048).keyID("test-key").generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
