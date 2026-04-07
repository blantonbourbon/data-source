package com.data.service.core.security;

import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ApplicationClientPrincipal implements UserDetails, AuthenticatedPrincipal {

    private final String name;
    private final String certificateCn;
    private final Map<String, String> claims;
    private final List<? extends GrantedAuthority> authorities;

    public ApplicationClientPrincipal(String name,
                                      String certificateCn,
                                      Map<String, String> claims,
                                      List<? extends GrantedAuthority> authorities) {
        this.name = name;
        this.certificateCn = certificateCn;
        this.claims = claims;
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return certificateCn;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getCertificateCn() {
        return certificateCn;
    }

    public Map<String, String> getClaims() {
        return claims;
    }
}
