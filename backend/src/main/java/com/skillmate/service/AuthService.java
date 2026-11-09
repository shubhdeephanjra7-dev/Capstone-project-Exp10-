package com.skillmate.service;

import com.skillmate.dto.AuthDtos.*;
import com.skillmate.entity.Role;
import com.skillmate.entity.User;
import com.skillmate.exception.ApiException;
import com.skillmate.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users; private final PasswordEncoder encoder;
    public AuthService(UserRepository users, PasswordEncoder encoder) { this.users = users; this.encoder = encoder; }

    /** Public registration always creates a STUDENT; any role supplied by the client is ignored. */
    @Transactional
    public User register(RegisterRequest r) {
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) throw ApiException.conflict("An account with this email already exists");
        User u = new User();
        u.setName(r.name().trim()); u.setEmail(email); u.setPassword(encoder.encode(r.password())); u.setRole(Role.STUDENT);
        return users.save(u);
    }

    @Transactional(readOnly = true)
    public User login(LoginRequest r) {
        User u = users.findByEmailIgnoreCase(r.email().trim()).orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));
        if (!encoder.matches(r.password(), u.getPassword())) throw ApiException.unauthorized("Invalid email or password");
        return u;
    }
}
