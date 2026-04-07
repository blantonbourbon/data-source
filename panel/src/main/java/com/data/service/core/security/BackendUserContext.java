package com.data.service.core.security;

import java.util.List;
import java.util.Map;

public record BackendUserContext(
        String id,
        String username,
        String displayName,
        String email,
        List<String> groups,
        List<String> entitlements,
        List<String> permissions,
        Map<String, Object> dataScopes,
        Map<String, Object> claims
) {
}
