package com.smarttourplanner.admin;

import com.smarttourplanner.admin.AdminModels.Role;
import com.smarttourplanner.catalog.VersionedCache;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Admins and their roles are set in configuration, so there are no passwords or user tables to protect.
 * Cloudflare Access handles the login itself.
 */
@Configuration(proxyBeanMethods = false)
class AdminConfig {

    private static final Logger log = LoggerFactory.getLogger(AdminConfig.class);

    /** Turned on by setting the team domain and the app's audience tag from the Access dashboard. */
    @Bean
    @ConditionalOnProperty({"smarttour.admin.access.team-domain", "smarttour.admin.access.audience"})
    AccessTokenVerifier accessTokenVerifier(
            @Value("${smarttour.admin.access.team-domain}") String teamDomain,
            @Value("${smarttour.admin.access.audience}") String audience) {
        return AccessTokenVerifier.forTeam(teamDomain.strip(), audience.strip());
    }

    @Bean
    AdminGate adminGate(
            Environment environment,
            ObjectProvider<AccessTokenVerifier> accessTokens,
            @Value("${smarttour.admin.local-user:}") String localUser,
            @Value("${smarttour.admin.publishers:}") String publishers,
            @Value("${smarttour.admin.editors:}") String editors) {
        AccessTokenVerifier verifier = accessTokens.getIfAvailable();
        if (!localUser.isBlank() && (verifier != null || environment.acceptsProfiles(Profiles.of("prod")))) {
            throw new IllegalStateException(
                    "smarttour.admin.local-user skips the login, so it's only allowed in local development.");
        }
        if (verifier == null && localUser.isBlank()) {
            log.info("The admin panel is off. Set up Cloudflare Access to turn it on.");
        }
        return new AdminGate(verifier, localUser.isBlank() ? null : normalize(localUser), roles(publishers, editors));
    }

    @Bean
    WebMvcConfigurer adminApiGate(AdminGate gate) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(gate).addPathPatterns("/api/v1/admin/**");
            }
        };
    }

    @Bean
    PageRefresher pageRefresher(
            VersionedCache catalogCache,
            @Value("${smarttour.web.refresh-url:}") String refreshUrl,
            @Value("${smarttour.web.refresh-secret:}") String refreshSecret,
            @Value("${smarttour.catalog.version-check-interval:5s}") Duration versionCheckInterval) {
        return new PageRefresher(
                catalogCache,
                refreshUrl.isBlank() ? null : URI.create(refreshUrl),
                refreshSecret.isBlank() ? null : refreshSecret,
                versionCheckInterval.plusSeconds(1));
    }

    /** Someone listed in both lists is a publisher. */
    static Map<String, Role> roles(String publishers, String editors) {
        Map<String, Role> roles = new HashMap<>();
        emails(editors).forEach(email -> roles.put(email, Role.EDITOR));
        emails(publishers).forEach(email -> roles.put(email, Role.PUBLISHER));
        return roles;
    }

    private static List<String> emails(String list) {
        return Arrays.stream(list.split(",")).map(AdminConfig::normalize).filter(email -> !email.isEmpty()).toList();
    }

    private static String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
