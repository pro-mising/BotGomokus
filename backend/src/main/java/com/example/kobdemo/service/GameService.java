package com.example.kobdemo.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.kobdemo.dto.PlayRequest;
import com.example.kobdemo.mapper.GameRecordMapper;
import com.example.kobdemo.mapper.UserMapper;
import com.example.kobdemo.model.GameRecord;
import com.example.kobdemo.model.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class GameService {
    private final UserMapper userMapper;
    private final GameRecordMapper gameRecordMapper;
    private final Random random = new Random();

    public GameService(UserMapper userMapper, GameRecordMapper gameRecordMapper) {
        this.userMapper = userMapper;
        this.gameRecordMapper = gameRecordMapper;
    }

    public Map<String, Object> play(User user, PlayRequest request) {
        boolean win = random.nextBoolean();
        int delta = win ? 8 : -3;
        user.setRating(user.getRating() + delta);
        userMapper.updateById(user);

        String opponent = request.botId() == null ? "Guest Player" : "Bot #" + request.botId();
        List<List<Integer>> map = demoMap();
        GameRecord record = new GameRecord(
                null,
                user.getUsername(),
                opponent,
                win ? "win" : "lose",
                delta,
                map.toString(),
                LocalDateTime.now()
        );
        gameRecordMapper.insert(record);
        record.setMap(map);

        return Map.of("record", record, "rating", user.getRating());
    }

    public List<GameRecord> records() {
        return gameRecordMapper.selectList(new QueryWrapper<GameRecord>().orderByDesc("id"));
    }

    public List<Map<String, Object>> ranklist() {
        return userMapper.selectList(new QueryWrapper<User>().orderByDesc("rating")).stream()
                .map(user -> Map.<String, Object>of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "photo", user.getPhoto(),
                        "rating", user.getRating()
                ))
                .toList();
    }

    private List<List<Integer>> demoMap() {
        List<List<Integer>> map = new ArrayList<>();
        for (int r = 0; r < 9; r++) {
            List<Integer> row = new ArrayList<>();
            for (int c = 0; c < 9; c++) {
                boolean border = r == 0 || c == 0 || r == 8 || c == 8;
                boolean wall = border || ((r + c) % 7 == 0 && !(r == 7 && c == 1) && !(r == 1 && c == 7));
                row.add(wall ? 1 : 0);
            }
            map.add(row);
        }
        return map;
    }
}
