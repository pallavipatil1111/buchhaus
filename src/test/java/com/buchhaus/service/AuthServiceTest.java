package com.buchhaus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.buchhaus.domain.Role;
import com.buchhaus.domain.User;
import com.buchhaus.repository.UserRepository;
import com.buchhaus.security.JwtService;
import com.buchhaus.web.dto.LoginRequest;
import com.buchhaus.web.dto.RegisterRequest;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    private UserRepository users;
    private PasswordEncoder encoder;
    private AuthService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        encoder = mock(PasswordEncoder.class);
        service = new AuthService(users, encoder, new JwtService("test-secret-test-secret-test-secret-123", 60, Fixture.CLOCK));
    }

    @Test
    void duplicateEmailIsAConflict() {
        when(users.existsByEmailIgnoreCase("reader@buchhaus.local")).thenReturn(true);
        assertThatThrownBy(() -> service.register(new RegisterRequest("R", " Reader@BuchHaus.local ", "password1")))
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(users, never()).save(any());
    }

    @Test
    void badCredentialsGiveOneGenericError() {
        User admin = new User("Admin", "admin@buchhaus.local", "hash", Role.ADMIN);
        when(users.findByEmailIgnoreCase("admin@buchhaus.local")).thenReturn(Optional.of(admin));
        when(encoder.matches("nope", "hash")).thenReturn(false);
        Throwable wrong = org.assertj.core.api.Assertions.catchThrowable(
                () -> service.login(new LoginRequest("admin@buchhaus.local", "nope")));
        Throwable unknown = org.assertj.core.api.Assertions.catchThrowable(
                () -> service.login(new LoginRequest("ghost@buchhaus.local", "nope")));
        assertThat(((ApiException) wrong).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(wrong.getMessage()).isEqualTo(unknown.getMessage());
    }
}
