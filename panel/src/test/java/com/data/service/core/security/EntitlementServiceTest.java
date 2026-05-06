package com.data.service.core.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(classes = EntitlementServiceTest.PropertiesConfiguration.class,
        initializers = ConfigDataApplicationContextInitializer.class)
@ActiveProfiles("test")
class EntitlementServiceTest {

    @Autowired
    private PanelSecurityProperties securityProperties;

    @Test
    void normalizedGroupAuthorityGrantsMatchingEntityReadAccess() {
        EntitlementService service = entitlementService();
        Authentication authentication = authentication("GROUP_ACL_SERVICE_TEST_TRADES_READER");

        assertThat(service.canAccess(authentication, "trades", "read")).isTrue();
    }

    @Test
    void configuredEntityGroupGrantsMappedRouteEntityReadAccess() {
        EntitlementService service = entitlementService();
        OAuth2User principal = new DefaultOAuth2User(
                List.of(),
                Map.of(
                        "sub", "user-123",
                        "groups", List.of("acl_service_test_crypto_assets_reader")
                ),
                "sub"
        );
        Authentication authentication = new OAuth2AuthenticationToken(
                principal,
                principal.getAuthorities(),
                "pingfed"
        );

        assertThat(service.canAccess(authentication, "cryptoassets", "read")).isTrue();
    }

    @Test
    void unconfiguredEntityFallsBackToRouteEntityName() {
        EntitlementService service = entitlementService();
        Authentication authentication = authentication("GROUP_ACL_SERVICE_TEST_REPORTS_READER");

        assertThat(service.canAccess(authentication, "reports", "read")).isTrue();
    }

    @Test
    void environmentMismatchDeniesAccess() {
        EntitlementService service = entitlementService();
        Authentication authentication = authentication("GROUP_ACL_SERVICE_PROD_TRADES_READER");

        assertThat(service.canAccess(authentication, "trades", "read")).isFalse();
    }

    @Test
    void entityMismatchDeniesAccess() {
        EntitlementService service = entitlementService();
        Authentication authentication = authentication("GROUP_ACL_SERVICE_TEST_CRYPTOASSETS_READER");

        assertThat(service.canAccess(authentication, "trades", "read")).isFalse();
    }

    @Test
    void writerRoleGrantsReadAndWriteButNotDelete() {
        EntitlementService service = entitlementService();
        Authentication authentication = authentication("GROUP_ACL_SERVICE_TEST_TRADES_WRITER");

        assertThat(service.canAccess(authentication, "trades", "read")).isTrue();
        assertThat(service.canAccess(authentication, "trades", "write")).isTrue();
        assertThat(service.canAccess(authentication, "trades", "delete")).isFalse();
    }

    @Test
    void applicationClientKeepsGrafanaEntityAccessWorking() {
        EntitlementService service = entitlementService();
        ApplicationClientPrincipal principal = new ApplicationClientPrincipal(
                "grafana",
                "grafana-test",
                Map.of("environment", "test"),
                List.of(new SimpleGrantedAuthority("ROLE_APP_GRAFANA"))
        );
        Authentication authentication = new TestingAuthenticationToken(
                principal,
                "",
                "ROLE_APP_GRAFANA"
        );

        assertThat(service.canAccess(authentication, "trades", "read")).isTrue();
    }

    @Test
    void localAuthDisabledAllowsUserApiWithoutAuthentication() {
        PanelSecurityProperties securityProperties = new PanelSecurityProperties();
        securityProperties.getLocalDev().setAuthDisabled(true);
        EntitlementService service = new EntitlementService(securityProperties);

        assertThat(service.canAccess(null, "trades", "read")).isTrue();
    }

    private EntitlementService entitlementService() {
        return new EntitlementService(securityProperties);
    }

    private Authentication authentication(String authority) {
        return new TestingAuthenticationToken("alice", "", authority);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(PanelSecurityProperties.class)
    static class PropertiesConfiguration {
    }
}
