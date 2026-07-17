package com.kob.backend.service.impl.record;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.kob.backend.mapper.RecordAnalysisMapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.Record;
import com.kob.backend.pojo.RecordAnalysis;
import com.kob.backend.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@Service
public class RecordSearchService {
    private static final String INDEX_NAME = "botgomoku_record";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RecordAnalysisMapper analysisMapper;

    @Value("${elasticsearch.enabled:true}")
    private boolean enabled;

    @Value("${elasticsearch.url:http://10.119.6.66:9200}")
    private String elasticsearchUrl;

    @Value("${elasticsearch.username:}")
    private String elasticsearchUsername;

    @Value("${elasticsearch.password:}")
    private String elasticsearchPassword;

    private volatile boolean indexReady = false;

    public SearchResult search(String keyword, int page, int pageSize) {
        if (!enabled || keyword == null || keyword.isBlank()) return null;
        try {
            ensureIndex();
            JSONObject body = new JSONObject();
            body.put("from", Math.max(0, page - 1) * pageSize);
            body.put("size", pageSize);
            JSONObject multiMatch = new JSONObject();
            multiMatch.put("query", keyword.trim());
            multiMatch.put("fields", JSONArray.of("blackUsername^2", "whiteUsername^2", "resultText", "keyMoment^2", "summary"));
            multiMatch.put("operator", "or");
            body.put("query", JSONObject.of("multi_match", multiMatch));
            body.put("highlight", buildHighlight());
            body.put("sort", JSONArray.of(JSONObject.of("createdTime", JSONObject.of("order", "desc"))));

            ResponseEntity<String> response = restTemplate.exchange(endpoint("/_search"), HttpMethod.POST,
                    new HttpEntity<>(body.toJSONString(), jsonHeaders()), String.class);
            return parseResponse(response.getBody());
        } catch (Exception e) {
            indexReady = false;
            return null;
        }
    }

    public void syncRecord(Record record) {
        if (!enabled || record == null || record.getId() == null) return;
        try {
            ensureIndex();
            restTemplate.exchange(endpoint("/_doc/" + record.getId()), HttpMethod.PUT,
                    new HttpEntity<>(buildDocument(record).toJSONString(), jsonHeaders()), String.class);
        } catch (Exception e) {
            indexReady = false;
        }
    }

    private JSONObject buildDocument(Record record) {
        User userA = userMapper.selectById(record.getAId());
        User userB = userMapper.selectById(record.getBId());
        RecordAnalysis analysis = analysisMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RecordAnalysis>().eq("record_id", record.getId()));
        JSONObject document = new JSONObject();
        document.put("id", record.getId());
        document.put("blackUserId", record.getAId());
        document.put("blackUsername", userA == null ? "Unknown" : userA.getUsername());
        document.put("whiteUserId", record.getBId());
        document.put("whiteUsername", userB == null ? "Unknown" : userB.getUsername());
        document.put("winner", analysis == null ? "" : analysis.getWinner());
        document.put("resultText", analysis == null ? "" : analysis.getSummary());
        document.put("keyMoment", analysis == null ? "" : analysis.getKeyMoment());
        document.put("summary", analysis == null ? "" : analysis.getSummary());
        document.put("steps", analysis == null ? 0 : analysis.getTotalSteps());
        document.put("highlightScore", analysis == null ? 0 : analysis.getHighlightScore());
        document.put("createdTime", formatDate(record));
        return document;
    }

    private JSONObject buildHighlight() {
        JSONObject highlight = new JSONObject();
        highlight.put("pre_tags", JSONArray.of("<em>"));
        highlight.put("post_tags", JSONArray.of("</em>"));
        highlight.put("fields", JSONObject.of(
                "blackUsername", JSONObject.of("number_of_fragments", 0),
                "whiteUsername", JSONObject.of("number_of_fragments", 0),
                "keyMoment", JSONObject.of("number_of_fragments", 0),
                "summary", JSONObject.of("fragment_size", 140, "number_of_fragments", 1)
        ));
        return highlight;
    }

    private SearchResult parseResponse(String text) {
        JSONObject response = JSONObject.parseObject(text);
        JSONObject hitsObject = response.getJSONObject("hits");
        long total = 0;
        if (hitsObject.get("total") instanceof JSONObject totalObject) total = totalObject.getLongValue("value");
        List<Integer> ids = new ArrayList<>();
        Map<Integer, JSONObject> highlights = new HashMap<>();
        JSONArray hits = hitsObject.getJSONArray("hits");
        if (hits != null) {
            for (Object item : hits) {
                JSONObject hit = (JSONObject) item;
                JSONObject source = hit.getJSONObject("_source");
                if (source != null && source.getInteger("id") != null) {
                    Integer id = source.getInteger("id");
                    ids.add(id);
                    highlights.put(id, buildHighlightJson(hit.getJSONObject("highlight")));
                }
            }
        }
        return new SearchResult(ids, highlights, total);
    }

    private JSONObject buildHighlightJson(JSONObject highlight) {
        JSONObject result = new JSONObject();
        if (highlight == null) return result;
        result.put("black_username", firstHighlight(highlight.getJSONArray("blackUsername")));
        result.put("white_username", firstHighlight(highlight.getJSONArray("whiteUsername")));
        result.put("key_moment", firstHighlight(highlight.getJSONArray("keyMoment")));
        result.put("summary", firstHighlight(highlight.getJSONArray("summary")));
        return result;
    }

    private String firstHighlight(JSONArray array) {
        if (array == null || array.isEmpty()) return "";
        return array.getString(0);
    }

    private void ensureIndex() {
        if (indexReady) return;
        JSONObject properties = new JSONObject();
        properties.put("id", JSONObject.of("type", "integer"));
        properties.put("blackUserId", JSONObject.of("type", "integer"));
        properties.put("blackUsername", JSONObject.of("type", "text"));
        properties.put("whiteUserId", JSONObject.of("type", "integer"));
        properties.put("whiteUsername", JSONObject.of("type", "text"));
        properties.put("winner", JSONObject.of("type", "keyword"));
        properties.put("resultText", JSONObject.of("type", "text"));
        properties.put("keyMoment", JSONObject.of("type", "text"));
        properties.put("summary", JSONObject.of("type", "text"));
        properties.put("steps", JSONObject.of("type", "integer"));
        properties.put("highlightScore", JSONObject.of("type", "integer"));
        properties.put("createdTime", JSONObject.of("type", "date"));
        JSONObject body = JSONObject.of("settings", JSONObject.of("number_of_shards", 1, "number_of_replicas", 0),
                "mappings", JSONObject.of("properties", properties));
        try {
            restTemplate.exchange(baseUrl() + "/" + INDEX_NAME, HttpMethod.PUT,
                    new HttpEntity<>(body.toJSONString(), jsonHeaders()), String.class);
        } catch (Exception ignored) {
        }
        indexReady = true;
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (!elasticsearchUsername.isBlank()) headers.setBasicAuth(elasticsearchUsername, elasticsearchPassword);
        return headers;
    }

    private String endpoint(String path) {
        return baseUrl() + "/" + INDEX_NAME + path;
    }

    private String baseUrl() {
        return elasticsearchUrl.endsWith("/") ? elasticsearchUrl.substring(0, elasticsearchUrl.length() - 1) : elasticsearchUrl;
    }

    private String formatDate(Record record) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        return formatter.format(record.getCreatetime());
    }

    public record SearchResult(List<Integer> recordIds, Map<Integer, JSONObject> highlights, long total) {
    }
}
