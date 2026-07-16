package com.kob.backend.service.impl.utils;

public class RedisKey {
    private RedisKey() {
    }

    public static String onlineUser(Integer userId) {
        return "online:user:" + userId;
    }

    public static String latestBotEvaluationReport(Integer userId) {
        return "bot:evaluation:latest:" + userId;
    }

    public static String hotRecords() {
        return "record:hot";
    }

    public static String ranklistCache(String type) {
        return "ranklist:cache:" + type;
    }

    public static String ranklistUpdatedAt(String type) {
        return "ranklist:updated_at:" + type;
    }
}
