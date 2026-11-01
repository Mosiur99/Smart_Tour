package com.smarttourplanner.catalog;

import com.github.benmanes.caffeine.cache.Ticker;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

@Configuration(proxyBeanMethods = false)
class CatalogConfig {

    /** Lets browsers reuse their copy with a small 304 reply when the data hasn't changed. */
    @Bean
    FilterRegistrationBean<ShallowEtagHeaderFilter> apiEtagFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> filter = new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        filter.addUrlPatterns("/api/*");
        return filter;
    }

    @Bean
    VersionedCache catalogCache(
            CatalogRepository repository,
            @Value("${smarttour.catalog.version-check-interval:5s}") Duration versionCheckInterval,
            @Value("${smarttour.catalog.entry-lifetime:10m}") Duration entryLifetime) {
        return new VersionedCache(repository::currentVersion, versionCheckInterval, entryLifetime, Ticker.systemTicker());
    }
}
