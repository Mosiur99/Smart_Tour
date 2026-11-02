package com.smarttourplanner.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smarttourplanner.admin.AdminModels.Role;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.env.MockEnvironment;

class AdminConfigTest {

    private final AdminConfig config = new AdminConfig();

    @Test
    void refusesToSkipTheLoginInProduction() {
        MockEnvironment production = new MockEnvironment();
        production.setActiveProfiles("prod");

        assertThatThrownBy(() -> config.adminGate(production, noAccess(), "me@example.com", "me@example.com", ""))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refusesToSkipTheLoginWhenAccessIsSetUp() {
        StaticListableBeanFactory access = new StaticListableBeanFactory();
        access.addBean("accessTokenVerifier", new AccessTokens().verifier());

        assertThatThrownBy(() -> config.adminGate(new MockEnvironment(), access.getBeanProvider(AccessTokenVerifier.class),
                "me@example.com", "me@example.com", ""))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void startsWithTheAdminPanelOffWhenNothingIsSetUp() {
        assertThat(config.adminGate(new MockEnvironment(), noAccess(), "", "", "")).isNotNull();
    }

    @Test
    void readsRolesFromListsAndMakesPeopleInBothPublishers() {
        assertThat(AdminConfig.roles(" Owner@Example.com, both@example.com ,", "helper@example.com,both@example.com"))
                .isEqualTo(Map.of(
                        "owner@example.com", Role.PUBLISHER,
                        "both@example.com", Role.PUBLISHER,
                        "helper@example.com", Role.EDITOR));
    }

    private static org.springframework.beans.factory.ObjectProvider<AccessTokenVerifier> noAccess() {
        return new StaticListableBeanFactory().getBeanProvider(AccessTokenVerifier.class);
    }
}
