package com.data.service.core.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PanelSecurityPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withPropertyValues("spring.profiles.active=test")
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void bindsEntitlementsFromActiveProfileConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(PanelSecurityProperties.class);

            PanelSecurityProperties.Entitlements entitlements = context.getBean(PanelSecurityProperties.class)
                    .getEntitlements();

            assertThat(entitlements.getEnvironment()).isEqualTo("test");
            assertThat(entitlements.getGroupPrefix()).isEqualTo("acl_service");
            assertThat(entitlements.getEntityGroups())
                    .containsEntry("trades", "trades")
                    .containsEntry("cryptoassets", "crypto_assets");
            assertThat(entitlements.getRoleActions())
                    .containsEntry("reader", List.of("read"))
                    .containsEntry("writer", List.of("read", "write"))
                    .containsEntry("editor", List.of("read", "write"))
                    .containsEntry("exporter", List.of("export"))
                    .containsEntry("deleter", List.of("delete"))
                    .containsEntry("admin", List.of("read", "write", "delete", "export"))
                    .containsEntry("owner", List.of("read", "write", "delete", "export"));
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(PanelSecurityProperties.class)
    static class PropertiesConfiguration {
    }
}
