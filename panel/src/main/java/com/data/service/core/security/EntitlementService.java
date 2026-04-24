package com.data.service.core.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class EntitlementService {

    private static final Set<String> WILDCARD_ENTITIES = Set.of("ALL", "ANY");
    private static final Set<String> WILDCARD_ACTIONS = Set.of("ALL", "ANY");

    private final PanelSecurityProperties securityProperties;

    public EntitlementService(PanelSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public boolean canAccess(Authentication authentication, String entity, String action) {
        if (securityProperties.getLocalDev().isAuthDisabled()) {
            return true;
        }

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        if (isApplicationClient(authentication)) {
            return true;
        }

        String normalizedEntity = normalizeToken(entity);
        String normalizedAction = normalizeToken(action);
        if (normalizedEntity.isEmpty() || normalizedAction.isEmpty()) {
            return false;
        }

        return normalizedGroups(authentication).stream()
                .anyMatch(group -> groupGrants(group, normalizedEntity, normalizedAction));
    }

    private boolean isApplicationClient(Authentication authentication) {
        if (authentication.getPrincipal() instanceof ApplicationClientPrincipal) {
            return true;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_APP_GRAFANA"::equals);
    }

    private Set<String> normalizedGroups(Authentication authentication) {
        Set<String> groups = new LinkedHashSet<>();

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String authorityValue = authority.getAuthority();
            if (authorityValue == null || authorityValue.isBlank()) {
                continue;
            }

            if (authorityValue.startsWith("GROUP_")) {
                groups.add(normalizeToken(authorityValue.substring("GROUP_".length())));
            } else {
                groups.add(normalizeToken(authorityValue));
            }
        }

        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            Object rawGroups = oauth2User.getAttributes()
                    .get(securityProperties.getClaimMapping().getGroupsClaim());
            normalizeStringList(rawGroups).stream()
                    .map(this::normalizeToken)
                    .forEach(groups::add);
        }

        groups.remove("");
        return groups;
    }

    private boolean groupGrants(String normalizedGroup, String normalizedEntity, String normalizedAction) {
        PanelSecurityProperties.Entitlements entitlements = securityProperties.getEntitlements();
        String normalizedPrefix = normalizeToken(entitlements.getGroupPrefix());
        String expectedEnvironment = normalizeToken(entitlements.getEnvironment());

        if (normalizedPrefix.isEmpty() || expectedEnvironment.isEmpty()) {
            return false;
        }

        String prefix = normalizedPrefix + "_";
        if (!normalizedGroup.startsWith(prefix)) {
            return false;
        }

        String[] parts = normalizedGroup.substring(prefix.length()).split("_");
        if (parts.length < 3) {
            return false;
        }

        String groupEnvironment = parts[0];
        String groupRole = parts[parts.length - 1];
        String groupEntity = String.join("_", List.of(parts).subList(1, parts.length - 1));

        if (!expectedEnvironment.equals(groupEnvironment)) {
            return false;
        }

        if (!normalizedEntity.equals(groupEntity) && !WILDCARD_ENTITIES.contains(groupEntity)) {
            return false;
        }

        return actionsForRole(groupRole).stream()
                .anyMatch(roleAction -> normalizedAction.equals(roleAction) || WILDCARD_ACTIONS.contains(roleAction));
    }

    private List<String> actionsForRole(String normalizedRole) {
        Map<String, List<String>> configuredActions = securityProperties.getEntitlements().getRoleActions();
        List<String> actions = configuredActions.get(normalizedRole.toLowerCase(Locale.ROOT));
        if (actions == null) {
            actions = configuredActions.get(normalizedRole);
        }
        if (actions == null) {
            return List.of();
        }

        return actions.stream()
                .map(this::normalizeToken)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
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

    private String normalizeToken(String value) {
        if (value == null) {
            return "";
        }

        return value.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }
}
