package com.example.kobdemo.controller;

import com.example.kobdemo.dto.BotRequest;
import com.example.kobdemo.model.Bot;
import com.example.kobdemo.model.User;
import com.example.kobdemo.service.AuthService;
import com.example.kobdemo.service.BotService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bots")
public class BotController {
    private final AuthService authService;
    private final BotService botService;

    public BotController(AuthService authService, BotService botService) {
        this.authService = authService;
        this.botService = botService;
    }

    @GetMapping
    public List<Bot> list(@RequestHeader("X-Token") String token) {
        User user = authService.requireUser(token);
        return botService.list(user);
    }

    @PostMapping
    public Bot add(@RequestHeader("X-Token") String token, @RequestBody BotRequest request) {
        User user = authService.requireUser(token);
        return botService.add(user, request);
    }

    @PutMapping("/{id}")
    public Bot update(@RequestHeader("X-Token") String token, @PathVariable Integer id, @RequestBody BotRequest request) {
        User user = authService.requireUser(token);
        return botService.update(user, id, request);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> remove(@RequestHeader("X-Token") String token, @PathVariable Integer id) {
        User user = authService.requireUser(token);
        botService.remove(user, id);
        return Map.of("message", "success");
    }
}
