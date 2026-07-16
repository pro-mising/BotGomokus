package com.example.kobdemo.controller;

import com.example.kobdemo.dto.PlayRequest;
import com.example.kobdemo.model.GameRecord;
import com.example.kobdemo.model.User;
import com.example.kobdemo.service.AuthService;
import com.example.kobdemo.service.GameService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GameController {
    private final AuthService authService;
    private final GameService gameService;

    public GameController(AuthService authService, GameService gameService) {
        this.authService = authService;
        this.gameService = gameService;
    }

    @PostMapping("/games/play")
    public Map<String, Object> play(@RequestHeader("X-Token") String token, @RequestBody PlayRequest request) {
        User user = authService.requireUser(token);
        return gameService.play(user, request);
    }

    @GetMapping("/records")
    public List<GameRecord> records() {
        return gameService.records();
    }

    @GetMapping("/ranklist")
    public List<Map<String, Object>> ranklist() {
        return gameService.ranklist();
    }
}
