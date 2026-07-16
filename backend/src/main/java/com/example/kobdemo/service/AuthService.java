package com.example.kobdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.kobdemo.dto.LoginRequest;
import com.example.kobdemo.mapper.UserMapper;
import com.example.kobdemo.model.User;
import com.example.kobdemo.store.TokenStore;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {
    private final UserMapper userMapper;
    private final TokenStore tokenStore;

    public AuthService(UserMapper userMapper, TokenStore tokenStore) {
        this.userMapper = userMapper;
        this.tokenStore = tokenStore;
    }

    public Map<String, Object> login(LoginRequest request) {
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", request.username()));
        if (user == null || !user.getPassword().equals(request.password())) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        return Map.of("token", tokenStore.createToken(user), "user", safeUser(user));
    }

    public Map<String, Object> register(LoginRequest request) {
        if (request.username() == null || request.username().isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (request.password() == null || request.password().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        if (userMapper.selectCount(new QueryWrapper<User>().eq("username", request.username())) > 0) {
            throw new IllegalArgumentException("Username already exists");
        }
        User user = new User(
                null,
                request.username(),
                request.password(),
                "https://api.dicebear.com/8.x/thumbs/svg?seed=" + request.username(),
                1500
        );
        userMapper.insert(user);
        return Map.of("token", tokenStore.createToken(user), "user", safeUser(user));
    }

    public Map<String, Object> me(String token) {
        return safeUser(requireUser(token));
    }

    public User requireUser(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Missing token");
        }
        return tokenStore.findUserByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Please login again"));
    }

    public Map<String, Object> safeUser(User user) {
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "photo", user.getPhoto(),
                "rating", user.getRating()
        );
    }
}
