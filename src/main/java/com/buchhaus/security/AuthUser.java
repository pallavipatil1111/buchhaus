package com.buchhaus.security;

import com.buchhaus.domain.Role;

/** The authenticated caller, resolved from the JWT and injected with {@code @AuthenticationPrincipal}. */
public record AuthUser(Long id, String email, Role role) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
