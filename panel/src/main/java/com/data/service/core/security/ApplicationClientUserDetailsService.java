package com.data.service.core.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ApplicationClientUserDetailsService implements UserDetailsService {

    private final PanelSecurityProperties securityProperties;

    public ApplicationClientUserDetailsService(PanelSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    public UserDetails loadUserByUsername(String certificateCn) throws UsernameNotFoundException {
        PanelSecurityProperties.ApplicationClient client = securityProperties.findApplicationClientByCertificateCn(certificateCn)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown application client certificate CN: " + certificateCn));

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (client.getAuthorities().isEmpty() && client.getName() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_APP_" + client.getName().trim().toUpperCase(Locale.ROOT)));
        } else {
            client.getAuthorities().stream()
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
        }

        return new ApplicationClientPrincipal(
                client.getName(),
                client.getCertificateCn(),
                client.getClaims(),
                authorities
        );
    }
}
