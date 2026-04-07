package com.data.service.core.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class BackendUserContextMapper {

    private final PanelSecurityProperties securityProperties;

    public BackendUserContextMapper(PanelSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public BackendUserContext toBackendUserContext(OAuth2User user) {
        Map<String, Object> claims = new LinkedHashMap<>(user.getAttributes());
        PanelSecurityProperties.ClaimMapping claimMapping = securityProperties.getClaimMapping();

        List<String> groups = normalizeStringList(claims.get(claimMapping.getGroupsClaim()));
        List<String> entitlements = normalizeStringList(claims.get(claimMapping.getEntitlementsClaim()));
        List<String> permissions = normalizeStringList(claims.get(claimMapping.getPermissionsClaim()));
        String username = firstNonBlank(
                extractString(claims, claimMapping.getUsernameClaim()),
                extractString(claims, claimMapping.getEmailClaim()),
                extractString(claims, "sub"),
                user.getName()
        );

        return new BackendUserContext(
                firstNonBlank(extractString(claims, "sub"), username),
                username,
                firstNonBlank(extractString(claims, claimMapping.getDisplayNameClaim()), username),
                extractString(claims, claimMapping.getEmailClaim()),
                groups,
                entitlements,
                permissions,
                normalizeMap(claims.get(claimMapping.getDataScopesClaim())),
                claims
        );
    }

    public Collection<? extends GrantedAuthority> toGrantedAuthorities(Map<String, Object> claims) {
        PanelSecurityProperties.ClaimMapping claimMapping = securityProperties.getClaimMapping();
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        normalizeStringList(claims.get(claimMapping.getGroupsClaim())).stream()
                .map(value -> "GROUP_" + normalizeAuthoritySegment(value))
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        normalizeStringList(claims.get(claimMapping.getEntitlementsClaim())).stream()
                .map(value -> "ENTITLEMENT_" + normalizeAuthoritySegment(value))
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        normalizeStringList(claims.get(claimMapping.getPermissionsClaim())).stream()
                .map(value -> "PERMISSION_" + normalizeAuthoritySegment(value))
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        return authorities;
    }

    public Map<String, Object> extractAttributes(Collection<? extends GrantedAuthority> authorities) {
        Map<String, Object> attributes = new LinkedHashMap<>();

        for (GrantedAuthority authority : authorities) {
            if (authority instanceof OidcUserAuthority oidcUserAuthority) {
                attributes.putAll(oidcUserAuthority.getIdToken().getClaims());
                if (oidcUserAuthority.getUserInfo() != null) {
                    attributes.putAll(oidcUserAuthority.getUserInfo().getClaims());
                }
            } else if (authority instanceof OAuth2UserAuthority oauth2UserAuthority) {
                attributes.putAll(oauth2UserAuthority.getAttributes());
            }
        }

        return attributes;
    }

    private String extractString(Map<String, Object> claims, String claimName) {
        if (claimName == null || claimName.isBlank()) {
            return null;
        }

        Object value = claims.get(claimName);
        if (value == null) {
            return null;
        }

        String candidate = String.valueOf(value).trim();
        return candidate.isEmpty() ? null : candidate;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return "unknown-user";
    }

    private List<String> normalizeStringList(Object value) {
        if (value == null) {
            return List.of();
        }

        Collection<?> values;
        if (value instanceof Collection<?> collection) {
            values = collection;
        } else if (value instanceof String stringValue) {
            values = List.of(stringValue.split(","));
        } else {
            values = List.of(value);
        }

        List<String> normalized = new ArrayList<>();
        for (Object element : values) {
            if (element == null) {
                continue;
            }

            String candidate = String.valueOf(element).trim();
            if (!candidate.isEmpty()) {
                normalized.add(candidate);
            }
        }

        return normalized.stream().distinct().toList();
    }

    private Map<String, Object> normalizeMap(Object value) {
        if (!(value instanceof Map<?, ?> mapValue)) {
            return Collections.emptyMap();
        }

        Map<String, Object> normalized = new LinkedHashMap<>();
        mapValue.forEach((key, mapEntryValue) -> {
            if (key != null) {
                normalized.put(String.valueOf(key), mapEntryValue);
            }
        });
        return normalized;
    }

    private String normalizeAuthoritySegment(String value) {
        return value.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_");
    }
}
