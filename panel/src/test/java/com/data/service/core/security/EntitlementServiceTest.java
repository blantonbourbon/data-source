package com.data.service.core.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EntitlementServiceTest {

    @Test
    void normalizedGroupAuthorityGrantsMatchingEntityReadAccess() {
        EntitlementService service = entitlementService("qa");
        Authentication authentication = authentication("GROUP_ACL_SERVICE_QA_TRADES_READER");

        assertThat(service.canAccess(authentication, "trades", "read")).isTrue();
    }

    @Test
    void rawOauthGroupClaimGrantsMatchingEntityReadAccess() {
        EntitlementService service = entitlementService("qa");
        OAuth2User principal = new DefaultOAuth2User(
                List.of(),
                Map.of(
                        "sub", "user-123",
                        "groups", List.of("acl_service_qa_cryptoassets_reader")
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
    void environmentMismatchDeniesAccess() {
        EntitlementService service = entitlementService("qa");
        Authentication authentication = authentication("GROUP_ACL_SERVICE_PROD_TRADES_READER");

        assertThat(service.canAccess(authentication, "trades", "read")).isFalse();
    }

    @Test
    void entityMismatchDeniesAccess() {
        EntitlementService service = entitlementService("qa");
        Authentication authentication = authentication("GROUP_ACL_SERVICE_QA_CRYPTOASSETS_READER");

        assertThat(service.canAccess(authentication, "trades", "read")).isFalse();
    }

    @Test
    void writerRoleGrantsReadAndWriteButNotDelete() {
        EntitlementService service = entitlementService("qa");
        Authentication authentication = authentication("GROUP_ACL_SERVICE_QA_TRADES_WRITER");

        assertThat(service.canAccess(authentication, "trades", "read")).isTrue();
        assertThat(service.canAccess(authentication, "trades", "write")).isTrue();
        assertThat(service.canAccess(authentication, "trades", "delete")).isFalse();
    }

    @Test
    void applicationClientKeepsGrafanaEntityAccessWorking() {
        EntitlementService service = entitlementService("qa");
        ApplicationClientPrincipal principal = new ApplicationClientPrincipal(
                "grafana",
                "grafana-test",
                Map.of("environment", "qa"),
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

    private EntitlementService entitlementService(String environment) {
        PanelSecurityProperties securityProperties = new PanelSecurityProperties();
        securityProperties.getEntitlements().setEnvironment(environment);
        return new EntitlementService(securityProperties);
    }

    private Authentication authentication(String authority) {
        return new TestingAuthenticationToken("alice", "", authority);
    }
}
