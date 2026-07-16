package com.example.kobdemo.store;

import com.example.kobdemo.mapper.UserMapper;
import com.example.kobdemo.model.User;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenStore {
    private final UserMapper userMapper;
    private final Map<String, Integer> tokens = new ConcurrentHashMap<>();

    public TokenStore(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public String createToken(User user) {
        String token = "demo-token-" + user.getId() + "-" + System.currentTimeMillis();
        tokens.put(token, user.getId());
        return token;
    }

    public Optional<User> findUserByToken(String token) {
        Integer userId = tokens.get(token);
        if (userId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(userMapper.selectById(userId));
    }
}
