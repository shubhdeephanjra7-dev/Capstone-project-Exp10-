package com.skillmate.controller;

import com.skillmate.dto.AuthDtos.*;
import com.skillmate.entity.User;
import com.skillmate.exception.ApiException;
import com.skillmate.repository.UserRepository;
import com.skillmate.security.JwtAuthFilter;
import com.skillmate.security.JwtService;
import com.skillmate.security.UserPrincipal;
import com.skillmate.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth; private final JwtService jwt; private final UserRepository users;
    private final boolean secureCookie;

    public AuthController(AuthService auth, JwtService jwt, UserRepository users, @Value("${skillmate.cookie.secure:false}") boolean secureCookie) {
        this.auth = auth; this.jwt = jwt; this.users = users; this.secureCookie = secureCookie;
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Valid @RequestBody RegisterRequest r) {
        User u = auth.register(r);
        return ResponseEntity.status(HttpStatus.CREATED).header(HttpHeaders.SET_COOKIE, cookie(jwt.generate(u), jwt.getTtl()).toString()).body(UserDto.from(u));
    }

    @PostMapping("/login")
    public ResponseEntity<UserDto> login(@Valid @RequestBody LoginRequest r) {
        User u = auth.login(r);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie(jwt.generate(u), jwt.getTtl()).toString()).body(UserDto.from(u));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString()).build();
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal UserPrincipal me) {
        return users.findById(me.getId()).map(UserDto::from).orElseThrow(() -> ApiException.unauthorized("Session expired"));
    }

    /** HttpOnly so JavaScript (and therefore XSS) cannot read the token; SameSite=Lax limits cross-site sends. */
    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(JwtAuthFilter.COOKIE_NAME, value).httpOnly(true).secure(secureCookie)
                .sameSite("Lax").path("/").maxAge(maxAge).build();
    }
}
