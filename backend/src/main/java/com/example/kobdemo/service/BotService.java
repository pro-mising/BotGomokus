package com.example.kobdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.kobdemo.dto.BotRequest;
import com.example.kobdemo.mapper.BotMapper;
import com.example.kobdemo.model.Bot;
import com.example.kobdemo.model.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BotService {
    private final BotMapper botMapper;

    public BotService(BotMapper botMapper) {
        this.botMapper = botMapper;
    }

    public List<Bot> list(User user) {
        return botMapper.selectList(
                new QueryWrapper<Bot>()
                        .eq("user_id", user.getId())
                        .orderByAsc("id")
        );
    }

    public Bot add(User user, BotRequest request) {
        validate(request);
        Bot bot = new Bot(null, user.getId(), request.title(), request.description(), request.code(), LocalDateTime.now());
        botMapper.insert(bot);
        return bot;
    }

    public Bot update(User user, Integer id, BotRequest request) {
        validate(request);
        Bot bot = requireOwnBot(user, id);
        bot.setTitle(request.title());
        bot.setDescription(request.description());
        bot.setCode(request.code());
        botMapper.updateById(bot);
        return bot;
    }

    public void remove(User user, Integer id) {
        requireOwnBot(user, id);
        botMapper.deleteById(id);
    }

    private Bot requireOwnBot(User user, Integer id) {
        Bot bot = botMapper.selectById(id);
        if (bot == null) {
            throw new IllegalArgumentException("Bot not found");
        }
        if (!bot.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("You can only edit your own bot");
        }
        return bot;
    }

    private void validate(BotRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("Bot title is required");
        }
        if (request.code() == null || request.code().isBlank()) {
            throw new IllegalArgumentException("Bot code is required");
        }
    }
}
