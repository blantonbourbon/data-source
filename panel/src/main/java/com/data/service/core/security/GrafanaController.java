package com.data.service.core.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/grafana")
public class GrafanaController {

    @GetMapping("/me")
    public GrafanaPrincipalResponse currentApplication(@AuthenticationPrincipal ApplicationClientPrincipal principal) {
        List<String> authorities = principal.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .toList();

        return new GrafanaPrincipalResponse(
                principal.getName(),
                principal.getCertificateCn(),
                authorities,
                principal.getClaims()
        );
    }

    public record GrafanaPrincipalResponse(
            String name,
            String certificateCn,
            List<String> authorities,
            Map<String, String> claims
    ) {
    }
}
