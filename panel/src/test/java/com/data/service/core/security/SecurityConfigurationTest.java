package com.data.service.core.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigurationTest {

    @Test
    void withMappedOidcAuthoritiesAddsClaimDerivedAuthorities() {
        PanelSecurityProperties securityProperties = new PanelSecurityProperties();
        BackendUserContextMapper userContextMapper = new BackendUserContextMapper(securityProperties);

        OidcIdToken idToken = new OidcIdToken(
                "id-token",
                Instant.parse("2026-04-23T00:00:00Z"),
                Instant.parse("2026-04-23T01:00:00Z"),
                Map.of(
                        "sub", "user-123",
                        "preferred_username", "alice",
                        "name", "Alice Example",
                        "email", "alice@example.com"
                )
        );
        OidcUserInfo userInfo = new OidcUserInfo(Map.of(
                "groups", List.of("trading"),
                "permissions", List.of("module:data-explorer")
        ));
        OidcUser delegateUser = new DefaultOidcUser(
                List.of(new OidcUserAuthority(idToken, userInfo)),
                idToken,
                userInfo,
                "preferred_username"
        );

        OidcUser mappedUser = SecurityConfiguration.withMappedOidcAuthorities(
                new OidcUserRequest(clientRegistration(), accessToken(), idToken),
                delegateUser,
                userContextMapper
        );

        assertThat(mappedUser.getName()).isEqualTo("alice");
        assertThat(mappedUser.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("GROUP_TRADING", "PERMISSION_MODULE_DATA_EXPLORER");
    }

    private ClientRegistration clientRegistration() {
        return ClientRegistration.withRegistrationId("pingfed")
                .clientId("test-client")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("https://idp.example.test/auth")
                .tokenUri("https://idp.example.test/token")
                .userInfoUri("https://idp.example.test/userinfo")
                .userNameAttributeName("preferred_username")
                .jwkSetUri("https://idp.example.test/certs")
                .build();
    }

    private OAuth2AccessToken accessToken() {
        return new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "access-token",
                Instant.parse("2026-04-23T00:00:00Z"),
                Instant.parse("2026-04-23T01:00:00Z"),
                Set.of("openid", "profile", "email")
        );
    }
}
