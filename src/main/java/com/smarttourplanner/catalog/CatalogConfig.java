package com.smarttourplanner.catalog;

import com.github.benmanes.caffeine.cache.Ticker;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class CatalogConfig {

    @Bean
    VersionedCache catalogCache(
            CatalogRepository repository,
            @Value("${smarttour.catalog.version-check-interval:5s}") Duration versionCheckInterval,
            @Value("${smarttour.catalog.entry-lifetime:10m}") Duration entryLifetime) {
        return new VersionedCache(repository::currentVersion, versionCheckInterval, entryLifetime, Ticker.systemTicker());
    }
}
