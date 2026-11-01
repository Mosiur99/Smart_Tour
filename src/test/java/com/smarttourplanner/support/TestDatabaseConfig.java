package com.smarttourplanner.support;

import javax.sql.DataSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class TestDatabaseConfig {

    @Bean
    DataSource dataSource() {
        return TestDatabase.createEmpty();
    }
}
