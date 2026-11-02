package com.smarttourplanner.admin;

import com.smarttourplanner.admin.AdminModels.AdminUser;
import com.smarttourplanner.admin.AdminModels.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Lets a request into the admin API only when it comes from a signed-in admin. Cloudflare Access
 * signs people in. This checks its token and looks up the person's role.
 */
class AdminGate implements HandlerInterceptor {

    static final String USER_ATTRIBUTE = "smarttour.admin.user";

    private static final Set<String> READS = Set.of(HttpMethod.GET.name(), HttpMethod.HEAD.name());
    private static final Set<String> SAME_SITE = Set.of("same-origin", "none");

    private final @Nullable AccessTokenVerifier verifier;
    private final @Nullable String localUser;
    private final Map<String, Role> roles;

    /**
     * @param verifier checks Cloudflare Access tokens, or null when Access isn't set up
     * @param localUser treats every request as this person, for local development only
     * @param roles each admin's email and role
     */
    AdminGate(@Nullable AccessTokenVerifier verifier, @Nullable String localUser, Map<String, Role> roles) {
        this.verifier = verifier;
        this.localUser = localUser;
        this.roles = Map.copyOf(roles);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        if (!READS.contains(request.getMethod())) {
            refuseOtherSites(request);
        }
        String email = signedInEmail(request).orElseThrow(AdminException::notSignedIn);
        Role role = Optional.ofNullable(roles.get(email))
                .orElseThrow(() -> AdminException.forbidden("This account isn't an admin."));
        request.setAttribute(USER_ATTRIBUTE, new AdminUser(email, role));
        return true;
    }

    /**
     * The Access login is a cookie, so a page on another site could try to send changes with it.
     * Browsers say where a request came from, and changes from anywhere else are refused.
     */
    private static void refuseOtherSites(HttpServletRequest request) {
        String site = request.getHeader("Sec-Fetch-Site");
        if (site != null && !SAME_SITE.contains(site.toLowerCase(Locale.ROOT))) {
            throw AdminException.forbidden("Changes must come from the admin panel itself.");
        }
    }

    private Optional<String> signedInEmail(HttpServletRequest request) {
        if (localUser != null) {
            return Optional.of(localUser);
        }
        String token = request.getHeader(AccessTokenVerifier.HEADER);
        if (verifier == null || token == null) {
            return Optional.empty();
        }
        return verifier.verifiedEmail(token);
    }
}
