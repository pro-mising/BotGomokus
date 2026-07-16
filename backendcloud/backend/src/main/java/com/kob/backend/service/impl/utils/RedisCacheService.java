package com.kob.backend.service.impl.utils;

import com.alibaba.fastjson2.JSONObject;
import com.kob.backend.pojo.BotEvaluationReport;
import com.kob.backend.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

@Service
public class RedisCacheService {
    private static final Duration ONLINE_TTL = Duration.ofSeconds(120);
    private static final Duration LATEST_REPORT_TTL = Duration.ofMinutes(30);
    private static final Duration RANKLIST_TTL = Duration.ofMinutes(10);

    @Autowired
    private StringRedisTemplate redisTemplate;

    public void markOnline(User user) {
        if (user == null) return;
        JSONObject value = new JSONObject();
        value.put("userId", user.getId());
        value.put("username", user.getUsername());
        value.put("photo", user.getPhoto());
        value.put("connectedAt", new Date());
        safeSet(RedisKey.onlineUser(user.getId()), value.toJSONString(), ONLINE_TTL);
    }

    public void markOffline(Integer userId) {
        if (userId == null) return;
        safeDelete(RedisKey.onlineUser(userId));
    }

    public boolean isOnline(Integer userId) {
        if (userId == null) return false;
        try {
            Boolean exists = redisTemplate.hasKey(RedisKey.onlineUser(userId));
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            return false;
        }
    }

    public void cacheLatestEvaluationReport(Integer userId, Integer reportId) {
        if (userId == null || reportId == null) return;
        safeSet(RedisKey.latestBotEvaluationReport(userId), reportId.toString(), LATEST_REPORT_TTL);
    }

    public Integer getLatestEvaluationReportId(Integer userId) {
        if (userId == null) return null;
        try {
            String value = redisTemplate.opsForValue().get(RedisKey.latestBotEvaluationReport(userId));
            if (value == null || value.isBlank()) return null;
            return Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    public void cacheHotRecord(Integer recordId, double score) {
        if (recordId == null) return;
        try {
            redisTemplate.opsForZSet().add(RedisKey.hotRecords(), recordId.toString(), score);
        } catch (Exception ignored) {
        }
    }

    public Set<String> getHotRecordIds(int limit) {
        try {
            Set<String> values = redisTemplate.opsForZSet().reverseRange(RedisKey.hotRecords(), 0, Math.max(0, limit - 1));
            return values == null ? Collections.emptySet() : values;
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    public void cacheRanklist(String type, String json, String updatedAt) {
        if (type == null || json == null) return;
        safeSet(RedisKey.ranklistCache(type), json, RANKLIST_TTL);
        if (updatedAt != null) safeSet(RedisKey.ranklistUpdatedAt(type), updatedAt, RANKLIST_TTL);
    }

    public String getCachedRanklist(String type) {
        if (type == null) return null;
        try {
            return redisTemplate.opsForValue().get(RedisKey.ranklistCache(type));
        } catch (Exception e) {
            return null;
        }
    }

    public String getRanklistUpdatedAt(String type) {
        if (type == null) return null;
        try {
            return redisTemplate.opsForValue().get(RedisKey.ranklistUpdatedAt(type));
        } catch (Exception e) {
            return null;
        }
    }

    private void safeSet(String key, String value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (Exception ignored) {
        }
    }

    private void safeDelete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception ignored) {
        }
    }
}
