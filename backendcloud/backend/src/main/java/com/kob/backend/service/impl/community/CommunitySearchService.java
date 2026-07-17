package com.kob.backend.service.impl.community;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.kob.backend.mapper.CommunityCommentMapper;
import com.kob.backend.mapper.CommunityPostMapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.CommunityComment;
import com.kob.backend.pojo.CommunityPost;
import com.kob.backend.pojo.User;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
public class CommunitySearchService {
    private static final String INDEX_NAME = "botgomoku_community_post";
    private static final int PAGE_SIZE = 10;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CommunityCommentMapper commentMapper;

    @Autowired
    private CommunityPostMapper postMapper;

    @Value("${elasticsearch.enabled:true}")
    private boolean enabled;

    @Value("${elasticsearch.url:http://10.119.6.66:9200}")
    private String elasticsearchUrl;

    @Value("${elasticsearch.username:}")
    private String elasticsearchUsername;

    @Value("${elasticsearch.password:}")
    private String elasticsearchPassword;

    private volatile boolean indexReady = false;
    private volatile boolean indexWarmedUp = false;

    public boolean isEnabled() {
        return enabled;
    }

    public SearchResult search(String keyword, String sort, int page) {
        if (!enabled) return null;
        try {
            ensureIndex();
            JSONObject body = buildSearchBody(keyword, sort, page);
            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint("/_search"),
                    HttpMethod.POST,
                    new HttpEntity<>(body.toJSONString(), jsonHeaders()),
                    String.class
            );
            return parseSearchResponse(response.getBody());
        } catch (Exception e) {
            indexReady = false;
            indexWarmedUp = false;
            return null;
        }
    }

    public void syncPost(CommunityPost post) {
        if (!enabled || post == null || post.getId() == null) return;
        try {
            ensureIndex();
            putDocument(post);
        } catch (Exception ignored) {
            indexReady = false;
            indexWarmedUp = false;
        }
    }

    public void deletePost(Integer postId) {
        if (!enabled || postId == null) return;
        try {
            ensureIndex();
            restTemplate.exchange(
                    endpoint("/_doc/" + postId),
                    HttpMethod.DELETE,
                    new HttpEntity<>("", jsonHeaders()),
                    String.class
            );
        } catch (Exception ignored) {
            indexReady = false;
            indexWarmedUp = false;
        }
    }

    private void ensureIndex() {
        if (indexReady) return;
        JSONObject body = new JSONObject();

        JSONObject settings = new JSONObject();
        settings.put("number_of_shards", 1);
        settings.put("number_of_replicas", 0);
        body.put("settings", settings);

        JSONObject properties = new JSONObject();
        properties.put("id", JSONObject.of("type", "integer"));
        properties.put("userId", JSONObject.of("type", "integer"));
        properties.put("username", JSONObject.of("type", "text", "fields", JSONObject.of("keyword", JSONObject.of("type", "keyword"))));
        properties.put("title", JSONObject.of("type", "text"));
        properties.put("content", JSONObject.of("type", "text"));
        properties.put("tag", JSONObject.of("type", "keyword"));
        properties.put("likeCount", JSONObject.of("type", "integer"));
        properties.put("commentCount", JSONObject.of("type", "integer"));
        properties.put("createTime", JSONObject.of("type", "date"));
        body.put("mappings", JSONObject.of("properties", properties));

        try {
            restTemplate.exchange(
                    baseUrl() + "/" + INDEX_NAME,
                    HttpMethod.PUT,
                    new HttpEntity<>(body.toJSONString(), jsonHeaders()),
                    String.class
            );
        } catch (Exception ignored) {
        }
        indexReady = true;
        warmUpIndex();
    }

    private void warmUpIndex() {
        if (indexWarmedUp) return;
        try {
            for (CommunityPost post : postMapper.selectList(null)) {
                putDocument(post);
            }
            indexWarmedUp = true;
        } catch (Exception ignored) {
            indexWarmedUp = false;
        }
    }

    private void putDocument(CommunityPost post) {
        if (post == null || post.getId() == null) return;
        JSONObject document = buildPostDocument(post);
        restTemplate.exchange(
                endpoint("/_doc/" + post.getId()),
                HttpMethod.PUT,
                new HttpEntity<>(document.toJSONString(), jsonHeaders()),
                String.class
        );
    }

    private JSONObject buildSearchBody(String keyword, String sort, int page) {
        JSONObject body = new JSONObject();
        int from = Math.max(0, page - 1) * PAGE_SIZE;
        body.put("from", from);
        body.put("size", PAGE_SIZE);

        String trimmedKeyword = keyword == null ? "" : keyword.trim();
        if (trimmedKeyword.isEmpty()) {
            body.put("query", JSONObject.of("match_all", new JSONObject()));
        } else {
            JSONObject multiMatch = new JSONObject();
            multiMatch.put("query", trimmedKeyword);
            multiMatch.put("fields", JSONArray.of("title^3", "content^2", "username"));
            multiMatch.put("type", "best_fields");
            multiMatch.put("operator", "or");
            body.put("query", JSONObject.of("multi_match", multiMatch));
            body.put("highlight", buildHighlight());
        }

        JSONArray sorts = new JSONArray();
        if ("likes".equals(sort)) {
            sorts.add(JSONObject.of("likeCount", JSONObject.of("order", "desc")));
        }
        sorts.add(JSONObject.of("createTime", JSONObject.of("order", "desc")));
        body.put("sort", sorts);
        return body;
    }

    private JSONObject buildHighlight() {
        JSONObject highlight = new JSONObject();
        highlight.put("pre_tags", JSONArray.of("<em>"));
        highlight.put("post_tags", JSONArray.of("</em>"));
        highlight.put("fields", JSONObject.of(
                "title", JSONObject.of("number_of_fragments", 0),
                "content", JSONObject.of("fragment_size", 140, "number_of_fragments", 1),
                "username", JSONObject.of("number_of_fragments", 0)
        ));
        return highlight;
    }

    private SearchResult parseSearchResponse(String responseText) {
        JSONObject response = JSONObject.parseObject(responseText);
        JSONObject hitsObject = response.getJSONObject("hits");
        long total = 0L;
        if (hitsObject.get("total") instanceof JSONObject totalObject) {
            total = totalObject.getLongValue("value");
        }

        List<Integer> postIds = new ArrayList<>();
        Map<Integer, JSONObject> highlights = new HashMap<>();
        JSONArray hits = hitsObject.getJSONArray("hits");
        if (hits != null) {
            for (Object hitObject : hits) {
                JSONObject hit = (JSONObject) hitObject;
                JSONObject source = hit.getJSONObject("_source");
                Integer postId = source.getInteger("id");
                if (postId == null) continue;
                postIds.add(postId);
                highlights.put(postId, buildHighlightJson(hit.getJSONObject("highlight")));
            }
        }
        return new SearchResult(postIds, highlights, total);
    }

    private JSONObject buildHighlightJson(JSONObject highlight) {
        JSONObject result = new JSONObject();
        if (highlight == null) return result;
        result.put("title", firstHighlight(highlight.getJSONArray("title")));
        result.put("content", firstHighlight(highlight.getJSONArray("content")));
        result.put("username", firstHighlight(highlight.getJSONArray("username")));
        return result;
    }

    private String firstHighlight(JSONArray array) {
        if (array == null || array.isEmpty()) return "";
        return array.getString(0);
    }

    private JSONObject buildPostDocument(CommunityPost post) {
        User user = userMapper.selectById(post.getUserId());
        QueryWrapper<CommunityComment> query = new QueryWrapper<>();
        query.eq("post_id", post.getId());

        JSONObject document = new JSONObject();
        document.put("id", post.getId());
        document.put("userId", post.getUserId());
        document.put("username", user == null ? "Unknown" : user.getUsername());
        document.put("title", post.getTitle());
        document.put("content", post.getContent());
        document.put("tag", post.getTag());
        document.put("likeCount", post.getLikes() == null ? 0 : post.getLikes());
        document.put("commentCount", commentMapper.selectCount(query));
        document.put("createTime", formatDate(post));
        return document;
    }

    private String formatDate(CommunityPost post) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        return formatter.format(post.getCreatetime());
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (!elasticsearchUsername.isBlank()) {
            headers.setBasicAuth(elasticsearchUsername, elasticsearchPassword);
        }
        return headers;
    }

    private String endpoint(String path) {
        return baseUrl() + "/" + INDEX_NAME + path;
    }

    private String baseUrl() {
        return elasticsearchUrl.endsWith("/")
                ? elasticsearchUrl.substring(0, elasticsearchUrl.length() - 1)
                : elasticsearchUrl;
    }

    public record SearchResult(List<Integer> postIds, Map<Integer, JSONObject> highlights, long total) {
    }
}
