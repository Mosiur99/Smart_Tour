package com.smarttourplanner.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;

class AccessTokenVerifierTest {

    private final AccessTokens tokens = new AccessTokens();
    private final AccessTokenVerifier verifier = tokens.verifier();

    @Test
    void returnsTheEmailFromAValidToken() {
        assertThat(verifier.verifiedEmail(tokens.forEmail("Owner@Example.com"))).contains("owner@example.com");
    }

    @Test
    void refusesATokenSignedWithAnotherKey() {
        String forged = AccessTokens.sign(tokens.claims("owner@example.com").build(), AccessTokens.generate());
        assertThat(verifier.verifiedEmail(forged)).isEmpty();
    }

    @Test
    void refusesATokenForAnotherApp() {
        assertThat(verifier.verifiedEmail(tokens.sign(tokens.claims("owner@example.com").audience("other-app").build())))
                .isEmpty();
    }

    @Test
    void refusesATokenFromAnotherIssuer() {
        JWTClaimsSet claims = tokens.claims("owner@example.com").issuer("https://evil.cloudflareaccess.com").build();
        assertThat(verifier.verifiedEmail(tokens.sign(claims))).isEmpty();
    }

    @Test
    void refusesAnExpiredToken() {
        Date anHourAgo = Date.from(Instant.now().minus(Duration.ofHours(1)));
        JWTClaimsSet claims = tokens.claims("owner@example.com").expirationTime(anHourAgo).build();
        assertThat(verifier.verifiedEmail(tokens.sign(claims))).isEmpty();
    }

    @Test
    void refusesATokenWithoutAnEmail() {
        JWTClaimsSet claims = tokens.claims("owner@example.com").claim("email", null).build();
        assertThat(verifier.verifiedEmail(tokens.sign(claims))).isEmpty();
    }

    @Test
    void refusesHalfFinishedSettings() {
        assertThatThrownBy(() -> AccessTokenVerifier.forTeam("team.cloudflareaccess.com", " "))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> AccessTokenVerifier.forTeam("", "aud"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refusesUnsignedAndBrokenTokens() {
        String unsigned = new PlainJWT(tokens.claims("owner@example.com").build()).serialize();
        assertThat(verifier.verifiedEmail(unsigned)).isEmpty();
        assertThat(verifier.verifiedEmail("not-a-token")).isEmpty();
    }
}
