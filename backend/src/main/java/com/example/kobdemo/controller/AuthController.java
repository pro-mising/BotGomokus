package com.example.kobdemo.controller;

import com.example.kobdemo.dto.LoginRequest;
import com.example.kobdemo.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody LoginRequest request) {
        return authService.register(request);
    }

    @GetMapping("/me")
    public Map<String, Object> me(@RequestHeader("X-Token") String token) {
        return authService.me(token);
    }
}
