package com.buchhaus.web.dto;

import com.buchhaus.domain.Role;
import com.buchhaus.domain.User;

public record UserView(Long id, String name, String email, Role role) {

    public static UserView of(User u) {
        return new UserView(u.getId(), u.getName(), u.getEmail(), u.getRole());
    }
}
