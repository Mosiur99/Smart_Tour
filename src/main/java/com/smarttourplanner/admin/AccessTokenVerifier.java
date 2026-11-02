package com.smarttourplanner.admin;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.net.MalformedURLException;
import java.net.URI;
import java.text.ParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks the signed token Cloudflare Access adds to every request that passed its login. The token is
 * checked here too, so a request that reaches the server some other way can't claim to be an admin.
 */
class AccessTokenVerifier {

    static final String HEADER = "Cf-Access-Jwt-Assertion";

    private static final Logger log = LoggerFactory.getLogger(AccessTokenVerifier.class);

    private final DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();

    AccessTokenVerifier(JWKSource<SecurityContext> keys, String issuer, String audience) {
        processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keys));
        processor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(
                audience, new JWTClaimsSet.Builder().issuer(issuer).build(), Set.of("email", "exp")));
    }

    /** Reads Cloudflare's public keys from the team's address, and keeps them cached. */
    static AccessTokenVerifier forTeam(String teamDomain, String audience) {
        if (teamDomain.isBlank() || audience.isBlank()) {
            throw new IllegalStateException("Set both the Cloudflare Access team domain and the app's audience tag.");
        }
        try {
            String issuer = "https://" + teamDomain;
            JWKSource<SecurityContext> keys = JWKSourceBuilder
                    .create(URI.create(issuer + "/cdn-cgi/access/certs").toURL())
                    .build();
            return new AccessTokenVerifier(keys, issuer, audience);
        } catch (MalformedURLException | IllegalArgumentException e) {
            throw new IllegalStateException("The Cloudflare Access team domain isn't valid: " + teamDomain, e);
        }
    }

    /** The signed-in email, or empty when the token is missing, forged, expired or for another app. */
    Optional<String> verifiedEmail(String token) {
        try {
            String email = processor.process(token, null).getStringClaim("email");
            return Optional.ofNullable(email).map(address -> address.toLowerCase(Locale.ROOT));
        } catch (ParseException | BadJOSEException e) {
            return Optional.empty();
        } catch (JOSEException e) {
            log.warn("Couldn't check an admin login token", e);
            return Optional.empty();
        }
    }
}
