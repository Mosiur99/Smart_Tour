package com.smarttourplanner.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration(proxyBeanMethods = false)
class SecurityConfig {

    @Bean
    NewPlanLimiter newPlanLimiter(
            @Value("${smarttour.limits.new-plans-per-client-per-minute:30}") int perClient,
            @Value("${smarttour.limits.new-plans-total-per-minute:600}") int total) {
        return new NewPlanLimiter(perClient, total);
    }

    @Bean
    ClientAddress clientAddress(@Value("${smarttour.proxy.behind-cloudflare:false}") boolean behindCloudflare) {
        return new ClientAddress(behindCloudflare);
    }

    @Bean
    FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilter() {
        FilterRegistrationBean<SecurityHeadersFilter> filter = new FilterRegistrationBean<>(new SecurityHeadersFilter());
        filter.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return filter;
    }

    @Bean
    FilterRegistrationBean<RequestSizeFilter> requestSizeFilter() {
        FilterRegistrationBean<RequestSizeFilter> filter = new FilterRegistrationBean<>(new RequestSizeFilter());
        filter.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return filter;
    }
}
