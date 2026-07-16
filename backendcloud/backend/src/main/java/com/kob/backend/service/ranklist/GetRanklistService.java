package com.kob.backend.service.ranklist;

import com.alibaba.fastjson2.JSONObject;

public interface GetRanklistService {
    JSONObject getList(Integer page);
    JSONObject getMultiList(String type, Integer page);
    void requestRefresh(String type);
    void requestRefreshAll();
}
