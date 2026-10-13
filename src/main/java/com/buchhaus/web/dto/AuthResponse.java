package com.buchhaus.web.dto;

public record AuthResponse(String token, long expiresIn, UserView user) {
}
