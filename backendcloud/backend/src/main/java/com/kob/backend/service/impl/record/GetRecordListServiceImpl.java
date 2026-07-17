package com.kob.backend.service.impl.record;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kob.backend.mapper.RecordAnalysisMapper;
import com.kob.backend.mapper.RecordFavoriteMapper;
import com.kob.backend.mapper.RecordMapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.Record;
import com.kob.backend.pojo.RecordAnalysis;
import com.kob.backend.pojo.RecordFavorite;
import com.kob.backend.pojo.User;
import com.kob.backend.service.impl.utils.RedisCacheService;
import com.kob.backend.service.impl.utils.UserDetailsImpl;
import com.kob.backend.service.record.GetRecordListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class GetRecordListServiceImpl implements GetRecordListService {
    private static final int PAGE_SIZE = 10;

    @Autowired
    private RecordMapper recordMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RecordAnalysisMapper analysisMapper;

    @Autowired
    private RecordFavoriteMapper favoriteMapper;

    @Autowired
    private RecordAnalysisService analysisService;

    @Autowired
    private RecordSearchService searchService;

    @Autowired
    private RedisCacheService redisCacheService;

    @Override
    public JSONObject getList(Integer page) {
        IPage<Record> recordIPage = new Page<>(page, PAGE_SIZE);
        QueryWrapper<Record> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("id");
        List<Record> records = recordMapper.selectPage(recordIPage, queryWrapper).getRecords();
        JSONObject resp = new JSONObject();

        List<JSONObject> items = new ArrayList<>();
        for (Record record : records) {
            items.add(recordJson(record, currentUserId()));
            analysisService.publish(record.getId());
        }
        resp.put("records", items);
        resp.put("record_count", recordMapper.selectCount(null));
        return resp;
    }

    @Override
    public JSONObject getCenterList(Map<String, String> data) {
        Integer userId = currentUserId();
        int page = Integer.parseInt(data.getOrDefault("page", "1"));
        String keyword = data.getOrDefault("keyword", "").trim();
        String result = data.getOrDefault("result", "all");
        String sort = data.getOrDefault("sort", "time");
        boolean favoriteOnly = "true".equals(data.getOrDefault("favorite_only", "false"));

        List<Record> records;
        long total;
        RecordSearchService.SearchResult searchResult = searchService.search(keyword, page, PAGE_SIZE);
        if (searchResult != null && !"hot".equals(sort) && "all".equals(result) && !favoriteOnly) {
            records = loadRecordsByIds(searchResult.recordIds());
            total = searchResult.total();
        } else {
            IPage<Record> recordPage = new Page<>(page, PAGE_SIZE);
            QueryWrapper<Record> query = buildQuery(result, sort, favoriteOnly, userId);
            if (!keyword.isBlank()) {
                List<Integer> userIds = findUserIds(keyword);
                query.and(wrapper -> {
                    if (!userIds.isEmpty()) wrapper.in("a_id", userIds).or().in("b_id", userIds);
                    else wrapper.eq("id", -1);
                });
            }
            IPage<Record> resultPage = recordMapper.selectPage(recordPage, query);
            records = resultPage.getRecords();
            total = resultPage.getTotal();
        }

        List<JSONObject> items = new ArrayList<>();
        for (Record record : records) {
            analysisService.ensureAnalysis(record.getId());
            JSONObject item = recordJson(record, userId);
            if (searchResult != null && searchResult.highlights() != null && searchResult.highlights().get(record.getId()) != null) {
                item.put("highlight", searchResult.highlights().get(record.getId()));
            }
            items.add(item);
        }

        JSONObject resp = new JSONObject();
        resp.put("records", items);
        resp.put("record_count", total);
        resp.put("hot_records", redisCacheService.getHotRecordIds(5));
        return resp;
    }

    @Override
    public Map<String, String> addFavorite(Integer recordId) {
        Integer userId = currentUserId();
        if (recordMapper.selectById(recordId) == null) return Map.of("error_message", "record not found");
        QueryWrapper<RecordFavorite> query = new QueryWrapper<>();
        query.eq("record_id", recordId).eq("user_id", userId);
        if (favoriteMapper.selectCount(query) == 0) {
            favoriteMapper.insert(new RecordFavorite(null, recordId, userId, new Date()));
        }
        analysisService.ensureAnalysis(recordId);
        return Map.of("error_message", "success");
    }

    @Override
    public Map<String, String> removeFavorite(Integer recordId) {
        Integer userId = currentUserId();
        QueryWrapper<RecordFavorite> query = new QueryWrapper<>();
        query.eq("record_id", recordId).eq("user_id", userId);
        favoriteMapper.delete(query);
        analysisService.ensureAnalysis(recordId);
        return Map.of("error_message", "success");
    }

    private QueryWrapper<Record> buildQuery(String result, String sort, boolean favoriteOnly, Integer userId) {
        QueryWrapper<Record> query = new QueryWrapper<>();
        if ("a_win".equals(result)) query.eq("loser", "B");
        else if ("b_win".equals(result)) query.eq("loser", "A");
        else if ("draw".equals(result)) query.eq("loser", "all");

        if (favoriteOnly) {
            QueryWrapper<RecordFavorite> favoriteQuery = new QueryWrapper<>();
            favoriteQuery.eq("user_id", userId);
            List<Integer> ids = favoriteMapper.selectList(favoriteQuery).stream().map(RecordFavorite::getRecordId).toList();
            if (ids.isEmpty()) query.eq("id", -1);
            else query.in("id", ids);
        }

        if ("steps".equals(sort)) query.orderByDesc("id");
        else if ("hot".equals(sort)) {
            Set<String> hotIdsText = redisCacheService.getHotRecordIds(100);
            List<Integer> hotIds = hotIdsText.stream().map(Integer::parseInt).toList();
            if (hotIds.isEmpty()) query.orderByDesc("id");
            else query.in("id", hotIds).last("order by field(id," + String.join(",", hotIdsText) + ")");
        } else query.orderByDesc("id");
        return query;
    }

    private List<Integer> findUserIds(String keyword) {
        QueryWrapper<User> query = new QueryWrapper<>();
        query.like("username", keyword);
        return userMapper.selectList(query).stream().map(User::getId).toList();
    }

    private List<Record> loadRecordsByIds(List<Integer> ids) {
        List<Record> records = new ArrayList<>();
        for (Integer id : ids) {
            Record record = recordMapper.selectById(id);
            if (record != null) records.add(record);
        }
        return records;
    }

    private JSONObject recordJson(Record record, Integer viewerId) {
        User userA = userMapper.selectById(record.getAId());
        User userB = userMapper.selectById(record.getBId());
        RecordAnalysis analysis = findAnalysis(record.getId());
        JSONObject item = new JSONObject();
        item.put("a_photo", userA == null ? "" : userA.getPhoto());
        item.put("a_username", userA == null ? "黑方" : userA.getUsername());
        item.put("b_photo", userB == null ? "" : userB.getPhoto());
        item.put("b_username", userB == null ? "白方" : userB.getUsername());
        item.put("result", resultText(record.getLoser()));
        item.put("record", record);
        item.put("analysis", analysisJson(analysis));
        item.put("favorite", isFavorite(record.getId(), viewerId));
        item.put("favorite_count", favoriteCount(record.getId()));
        return item;
    }

    private JSONObject analysisJson(RecordAnalysis analysis) {
        JSONObject json = new JSONObject();
        if (analysis == null) {
            json.put("summary", "分析排队中");
            json.put("key_moment", "正在生成分析");
            json.put("total_steps", 0);
            json.put("highlight_score", 0);
            return json;
        }
        json.put("summary", analysis.getSummary());
        json.put("key_moment", analysis.getKeyMoment());
        json.put("winner", analysis.getWinner());
        json.put("win_direction", analysis.getWinDirection());
        json.put("total_steps", analysis.getTotalSteps());
        json.put("key_step", analysis.getKeyStep());
        json.put("highlight_score", analysis.getHighlightScore());
        return json;
    }

    private RecordAnalysis findAnalysis(Integer recordId) {
        QueryWrapper<RecordAnalysis> query = new QueryWrapper<>();
        query.eq("record_id", recordId).last("limit 1");
        return analysisMapper.selectOne(query);
    }

    private boolean isFavorite(Integer recordId, Integer userId) {
        QueryWrapper<RecordFavorite> query = new QueryWrapper<>();
        query.eq("record_id", recordId).eq("user_id", userId);
        return favoriteMapper.selectCount(query) > 0;
    }

    private int favoriteCount(Integer recordId) {
        QueryWrapper<RecordFavorite> query = new QueryWrapper<>();
        query.eq("record_id", recordId);
        return favoriteMapper.selectCount(query).intValue();
    }

    private String resultText(String loser) {
        if ("A".equals(loser)) return "白方胜";
        if ("B".equals(loser)) return "黑方胜";
        return "平局";
    }

    private Integer currentUserId() {
        UsernamePasswordAuthenticationToken authenticationToken =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl loginUser = (UserDetailsImpl) authenticationToken.getPrincipal();
        return loginUser.getUser().getId();
    }
}
