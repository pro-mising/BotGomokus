package com.kob.backend.service.record;

import com.alibaba.fastjson2.JSONObject;

import java.util.Map;

public interface GetRecordListService {
    JSONObject getList(Integer page);
    JSONObject getCenterList(Map<String, String> data);
    Map<String, String> addFavorite(Integer recordId);
    Map<String, String> removeFavorite(Integer recordId);
}
