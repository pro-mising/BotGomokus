package com.kob.backend.service.impl.record;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kob.backend.config.RabbitMQConfig;
import com.kob.backend.mapper.RecordAnalysisMapper;
import com.kob.backend.mapper.RecordFavoriteMapper;
import com.kob.backend.mapper.RecordMapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.Record;
import com.kob.backend.pojo.RecordAnalysis;
import com.kob.backend.pojo.RecordFavorite;
import com.kob.backend.pojo.User;
import com.kob.backend.service.impl.utils.RedisCacheService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class RecordAnalysisService {
    private static final int SIZE = 15;

    @Autowired
    private RecordMapper recordMapper;

    @Autowired
    private RecordAnalysisMapper analysisMapper;

    @Autowired
    private RecordFavoriteMapper favoriteMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisCacheService redisCacheService;

    @Autowired
    private RecordSearchService recordSearchService;

    public void publish(Integer recordId) {
        if (recordId == null) return;
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.RECORD_EXCHANGE, RabbitMQConfig.RECORD_ANALYSIS_ROUTING_KEY,
                    new RecordAnalysisMessage(recordId));
        } catch (Exception e) {
            analyze(recordId);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.RECORD_ANALYSIS_QUEUE)
    public void consume(RecordAnalysisMessage message) {
        if (message == null || message.getRecordId() == null) return;
        analyze(message.getRecordId());
    }

    public RecordAnalysis ensureAnalysis(Integer recordId) {
        RecordAnalysis analysis = findAnalysis(recordId);
        if (analysis == null) {
            analyze(recordId);
            analysis = findAnalysis(recordId);
        }
        return analysis;
    }

    private void analyze(Integer recordId) {
        Record record = recordMapper.selectById(recordId);
        if (record == null) return;
        RecordAnalysis analysis = buildAnalysis(record);
        RecordAnalysis old = findAnalysis(recordId);
        if (old == null) analysisMapper.insert(analysis);
        else {
            analysis.setId(old.getId());
            analysisMapper.updateById(analysis);
        }
        cacheHotRecord(recordId, analysis);
        recordSearchService.syncRecord(record);
    }

    private RecordAnalysis buildAnalysis(Record record) {
        List<Integer> aSteps = parseSteps(record.getASteps());
        List<Integer> bSteps = parseSteps(record.getBSteps());
        int[][] board = new int[SIZE][SIZE];
        List<Move> moves = new ArrayList<>();
        int total = Math.max(aSteps.size(), bSteps.size());
        for (int i = 0; i < total; i++) {
            if (i < aSteps.size()) applyMove(board, moves, aSteps.get(i), 1, "黑方");
            if (i < bSteps.size()) applyMove(board, moves, bSteps.get(i), 2, "白方");
        }

        String winnerSide = winnerSide(record.getLoser());
        Move keyMove = findKeyMove(moves, winnerSide);
        String direction = keyMove == null ? "无" : detectDirection(board, keyMove.row, keyMove.col, keyMove.piece);
        String keyMoment = buildKeyMoment(keyMove, direction, winnerSide);
        String summary = buildSummary(record, winnerSide, total, direction, keyMoment);
        int favoriteCount = countFavorites(record.getId());
        int score = calculateHighlightScore(total, direction, favoriteCount, record.getLoser());
        Date now = new Date();
        return new RecordAnalysis(null, record.getId(), moves.size(), winnerSide, direction,
                keyMove == null ? 0 : keyMove.step, keyMoment, summary, score, now, now);
    }

    private void applyMove(int[][] board, List<Move> moves, Integer action, int piece, String side) {
        if (action == null || action < 0 || action >= SIZE * SIZE) return;
        int row = action / SIZE, col = action % SIZE;
        board[row][col] = piece;
        moves.add(new Move(moves.size() + 1, row, col, piece, side));
    }

    private Move findKeyMove(List<Move> moves, String winnerSide) {
        if ("平局".equals(winnerSide)) return moves.isEmpty() ? null : moves.get(moves.size() - 1);
        for (int i = moves.size() - 1; i >= 0; i--) {
            Move move = moves.get(i);
            if (move.side.equals(winnerSide)) return move;
        }
        return moves.isEmpty() ? null : moves.get(moves.size() - 1);
    }

    private String detectDirection(int[][] board, int row, int col, int piece) {
        int[][] dirs = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};
        String[] names = {"横向", "纵向", "右下斜向", "右上斜向"};
        for (int i = 0; i < dirs.length; i++) {
            int count = 1 + count(board, row, col, dirs[i][0], dirs[i][1], piece)
                    + count(board, row, col, -dirs[i][0], -dirs[i][1], piece);
            if (count >= 5) return names[i];
        }
        return "未形成五连";
    }

    private int count(int[][] board, int row, int col, int dx, int dy, int piece) {
        int res = 0;
        for (int x = row + dx, y = col + dy; x >= 0 && x < SIZE && y >= 0 && y < SIZE && board[x][y] == piece; x += dx, y += dy) {
            res++;
        }
        return res;
    }

    private String buildKeyMoment(Move move, String direction, String winnerSide) {
        if (move == null) return "暂无关键手分析";
        char col = (char) ('A' + move.col);
        int row = move.row + 1;
        if ("平局".equals(winnerSide)) return "第 " + move.step + " 手，双方进入平局收官。";
        return "第 " + move.step + " 手，" + winnerSide + "落在 " + col + row + "，通过" + direction + "完成胜利。";
    }

    private String buildSummary(Record record, String winnerSide, int totalSteps, String direction, String keyMoment) {
        User a = userMapper.selectById(record.getAId());
        User b = userMapper.selectById(record.getBId());
        String aName = a == null ? "黑方" : a.getUsername();
        String bName = b == null ? "白方" : b.getUsername();
        if ("平局".equals(winnerSide)) return aName + " 与 " + bName + " 共 " + totalSteps + " 手战成平局。" + keyMoment;
        return aName + " 对阵 " + bName + "，" + winnerSide + "共 " + totalSteps + " 手取胜，胜利方向为" + direction + "。" + keyMoment;
    }

    private int calculateHighlightScore(int totalSteps, String direction, int favoriteCount, String loser) {
        int score = Math.min(50, totalSteps) + favoriteCount * 10;
        if (!"未形成五连".equals(direction) && !"无".equals(direction)) score += 30;
        if ("all".equals(loser)) score += 12;
        return score;
    }

    private String winnerSide(String loser) {
        if ("A".equals(loser)) return "白方";
        if ("B".equals(loser)) return "黑方";
        return "平局";
    }

    private List<Integer> parseSteps(String steps) {
        List<Integer> result = new ArrayList<>();
        if (steps == null || steps.isBlank()) return result;
        for (String part : steps.split(",")) {
            if (part.isBlank()) continue;
            try {
                result.add(Integer.parseInt(part.trim()));
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    private RecordAnalysis findAnalysis(Integer recordId) {
        QueryWrapper<RecordAnalysis> query = new QueryWrapper<>();
        query.eq("record_id", recordId).last("limit 1");
        return analysisMapper.selectOne(query);
    }

    private int countFavorites(Integer recordId) {
        QueryWrapper<RecordFavorite> query = new QueryWrapper<>();
        query.eq("record_id", recordId);
        return favoriteMapper.selectCount(query).intValue();
    }

    private void cacheHotRecord(Integer recordId, RecordAnalysis analysis) {
        redisCacheService.cacheHotRecord(recordId, analysis.getHighlightScore() == null ? 0 : analysis.getHighlightScore());
    }

    private record Move(int step, int row, int col, int piece, String side) {
    }
}
