package com.kob.backend.service.impl.community;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kob.backend.consumer.WebSocketServer;
import com.kob.backend.mapper.CommunityCommentMapper;
import com.kob.backend.mapper.CommunityPostLikeMapper;
import com.kob.backend.mapper.CommunityPostMapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.CommunityComment;
import com.kob.backend.pojo.CommunityPost;
import com.kob.backend.pojo.CommunityPostLike;
import com.kob.backend.pojo.User;
import com.kob.backend.service.community.CommunityService;
import com.kob.backend.service.impl.utils.RedisCacheService;
import com.kob.backend.service.impl.utils.UserDetailsImpl;
import com.kob.backend.service.ranklist.GetRanklistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommunityServiceImpl implements CommunityService {
    @Autowired
    private CommunityPostMapper postMapper;

    @Autowired
    private CommunityCommentMapper commentMapper;

    @Autowired
    private CommunityPostLikeMapper likeMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisCacheService redisCacheService;

    @Autowired
    private CommunitySearchService communitySearchService;

    @Autowired
    private GetRanklistService ranklistService;

    private User currentUser() {
        UsernamePasswordAuthenticationToken authenticationToken =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl loginUser = (UserDetailsImpl) authenticationToken.getPrincipal();
        return loginUser.getUser();
    }

    private JSONObject userJson(Integer userId, Integer viewerId) {
        User user = userMapper.selectById(userId);
        JSONObject resp = new JSONObject();
        if (user == null) {
            resp.put("id", userId);
            resp.put("username", "Unknown");
            resp.put("photo", "");
        } else {
            resp.put("id", user.getId());
            resp.put("username", user.getUsername());
            resp.put("photo", user.getPhoto());
        }
        resp.put("online", isOnline(userId, viewerId));
        return resp;
    }

    private boolean isOnline(Integer userId, Integer viewerId) {
        if (userId == null) return false;
        if (userId.equals(viewerId)) return true;
        return redisCacheService.isOnline(userId) || WebSocketServer.isUserConnected(userId);
    }

    private JSONObject postJson(CommunityPost post, Integer viewerId) {
        JSONObject item = new JSONObject();
        item.put("id", post.getId());
        item.put("user_id", post.getUserId());
        item.put("title", post.getTitle());
        item.put("content", post.getContent());
        item.put("tag", post.getTag());
        item.put("likes", post.getLikes() == null ? 0 : post.getLikes());
        item.put("createtime", post.getCreatetime());
        item.put("author", userJson(post.getUserId(), viewerId));
        item.put("liked", isLiked(post.getId(), viewerId));
        item.put("can_delete", post.getUserId().equals(viewerId));

        QueryWrapper<CommunityComment> commentQuery = new QueryWrapper<>();
        commentQuery.eq("post_id", post.getId());
        item.put("comment_count", commentMapper.selectCount(commentQuery));
        return item;
    }

    private JSONObject postJson(CommunityPost post, Integer viewerId, JSONObject highlight) {
        JSONObject item = postJson(post, viewerId);
        if (highlight != null) {
            item.put("highlight_title", highlight.getString("title"));
            item.put("highlight_content", highlight.getString("content"));
            item.put("highlight_username", highlight.getString("username"));
        }
        return item;
    }

    private boolean isLiked(Integer postId, Integer userId) {
        QueryWrapper<CommunityPostLike> query = new QueryWrapper<>();
        query.eq("post_id", postId).eq("user_id", userId);
        return likeMapper.selectCount(query) > 0;
    }

    private Map<String, String> success() {
        Map<String, String> map = new HashMap<>();
        map.put("error_message", "success");
        return map;
    }

    private Map<String, String> error(String message) {
        Map<String, String> map = new HashMap<>();
        map.put("error_message", message);
        return map;
    }

    @Override
    public JSONObject getPostList(Integer page) {
        User user = currentUser();
        redisCacheService.markOnline(user);
        IPage<CommunityPost> postPage = new Page<>(page, 10);
        QueryWrapper<CommunityPost> query = new QueryWrapper<>();
        query.orderByDesc("id");
        List<CommunityPost> posts = postMapper.selectPage(postPage, query).getRecords();

        List<JSONObject> items = new ArrayList<>();
        for (CommunityPost post : posts) {
            items.add(postJson(post, user.getId()));
        }

        JSONObject resp = new JSONObject();
        resp.put("posts", items);
        resp.put("post_count", postMapper.selectCount(null));
        return resp;
    }

    @Override
    public JSONObject searchPostList(Map<String, String> data) {
        User user = currentUser();
        redisCacheService.markOnline(user);

        int page = Integer.parseInt(data.getOrDefault("page", "1"));
        String keyword = data.getOrDefault("keyword", "");
        String sort = data.getOrDefault("sort", "time");

        CommunitySearchService.SearchResult searchResult = communitySearchService.search(keyword, sort, page);
        if (searchResult != null) {
            List<JSONObject> items = new ArrayList<>();
            for (Integer postId : searchResult.postIds()) {
                CommunityPost post = postMapper.selectById(postId);
                if (post != null) {
                    items.add(postJson(post, user.getId(), searchResult.highlights().get(postId)));
                }
            }
            JSONObject resp = new JSONObject();
            resp.put("posts", items);
            resp.put("post_count", searchResult.total());
            resp.put("search_engine", "elasticsearch");
            return resp;
        }

        return searchPostListFromMysql(user.getId(), page, keyword, sort);
    }

    private JSONObject searchPostListFromMysql(Integer viewerId, int page, String keyword, String sort) {
        IPage<CommunityPost> postPage = new Page<>(page, 10);
        QueryWrapper<CommunityPost> query = new QueryWrapper<>();
        String trimmedKeyword = keyword == null ? "" : keyword.trim();
        if (!trimmedKeyword.isEmpty()) {
            List<Integer> userIds = new ArrayList<>();
            QueryWrapper<User> userQuery = new QueryWrapper<>();
            userQuery.like("username", trimmedKeyword);
            for (User user : userMapper.selectList(userQuery)) {
                userIds.add(user.getId());
            }
            query.and(wrapper -> {
                wrapper.like("title", trimmedKeyword).or().like("content", trimmedKeyword);
                if (!userIds.isEmpty()) wrapper.or().in("user_id", userIds);
            });
        }
        if ("likes".equals(sort)) query.orderByDesc("likes").orderByDesc("id");
        else query.orderByDesc("id");

        IPage<CommunityPost> resultPage = postMapper.selectPage(postPage, query);
        List<JSONObject> items = new ArrayList<>();
        for (CommunityPost post : resultPage.getRecords()) {
            items.add(postJson(post, viewerId));
        }

        JSONObject resp = new JSONObject();
        resp.put("posts", items);
        resp.put("post_count", resultPage.getTotal());
        resp.put("search_engine", "mysql");
        return resp;
    }

    @Override
    public JSONObject getPostDetail(Integer postId) {
        User user = currentUser();
        redisCacheService.markOnline(user);
        CommunityPost post = postMapper.selectById(postId);
        JSONObject resp = new JSONObject();
        if (post == null) {
            resp.put("error_message", "post not found");
            return resp;
        }
        resp.put("error_message", "success");
        resp.put("post", postJson(post, user.getId()));
        return resp;
    }

    @Override
    public Map<String, String> addPost(Map<String, String> data) {
        User user = currentUser();
        String title = data.get("title");
        String content = data.get("content");
        String tag = data.get("tag");

        if (title == null || title.trim().isEmpty()) return error("title cannot be empty");
        if (title.length() > 100) return error("title is too long");
        if (content == null || content.trim().isEmpty()) return error("content cannot be empty");
        if (content.length() > 3000) return error("content is too long");
        if (tag == null || tag.trim().isEmpty()) tag = "General";
        if (tag.length() > 30) return error("tag is too long");

        CommunityPost post = new CommunityPost(
                null,
                user.getId(),
                title.trim(),
                content.trim(),
                tag.trim(),
                0,
                new Date()
        );
        postMapper.insert(post);
        communitySearchService.syncPost(post);
        refreshCommunityRanklists();
        return success();
    }

    @Override
    public Map<String, String> removePost(Integer postId) {
        User user = currentUser();
        CommunityPost post = postMapper.selectById(postId);
        if (post == null) return error("post not found");
        if (!post.getUserId().equals(user.getId())) return error("permission denied");

        QueryWrapper<CommunityComment> commentQuery = new QueryWrapper<>();
        commentQuery.eq("post_id", postId);
        commentMapper.delete(commentQuery);

        QueryWrapper<CommunityPostLike> likeQuery = new QueryWrapper<>();
        likeQuery.eq("post_id", postId);
        likeMapper.delete(likeQuery);

        postMapper.deleteById(postId);
        communitySearchService.deletePost(postId);
        refreshCommunityRanklists();
        return success();
    }

    @Override
    public Map<String, String> likePost(Integer postId) {
        User user = currentUser();
        CommunityPost post = postMapper.selectById(postId);
        if (post == null) return error("post not found");
        if (isLiked(postId, user.getId())) return success();

        likeMapper.insert(new CommunityPostLike(null, postId, user.getId(), new Date()));
        post.setLikes((post.getLikes() == null ? 0 : post.getLikes()) + 1);
        postMapper.updateById(post);
        communitySearchService.syncPost(post);
        refreshCommunityRanklists();
        return success();
    }

    @Override
    public Map<String, String> unlikePost(Integer postId) {
        User user = currentUser();
        CommunityPost post = postMapper.selectById(postId);
        if (post == null) return error("post not found");

        QueryWrapper<CommunityPostLike> query = new QueryWrapper<>();
        query.eq("post_id", postId).eq("user_id", user.getId());
        if (likeMapper.delete(query) > 0) {
            post.setLikes(Math.max(0, (post.getLikes() == null ? 0 : post.getLikes()) - 1));
            postMapper.updateById(post);
            communitySearchService.syncPost(post);
            refreshCommunityRanklists();
        }
        return success();
    }

    @Override
    public JSONObject getCommentList(Integer postId) {
        User user = currentUser();
        redisCacheService.markOnline(user);
        QueryWrapper<CommunityComment> query = new QueryWrapper<>();
        query.eq("post_id", postId).orderByAsc("id");
        List<CommunityComment> comments = commentMapper.selectList(query);

        List<JSONObject> items = new ArrayList<>();
        for (CommunityComment comment : comments) {
            JSONObject item = new JSONObject();
            item.put("id", comment.getId());
            item.put("post_id", comment.getPostId());
            item.put("user_id", comment.getUserId());
            item.put("content", comment.getContent());
            item.put("createtime", comment.getCreatetime());
            item.put("author", userJson(comment.getUserId(), user.getId()));
            item.put("can_delete", comment.getUserId().equals(user.getId()));
            items.add(item);
        }

        JSONObject resp = new JSONObject();
        resp.put("comments", items);
        return resp;
    }

    @Override
    public Map<String, String> addComment(Map<String, String> data) {
        User user = currentUser();
        Integer postId = Integer.parseInt(data.get("post_id"));
        String content = data.get("content");

        if (postMapper.selectById(postId) == null) return error("post not found");
        if (content == null || content.trim().isEmpty()) return error("comment cannot be empty");
        if (content.length() > 1000) return error("comment is too long");

        commentMapper.insert(new CommunityComment(null, postId, user.getId(), content.trim(), new Date()));
        communitySearchService.syncPost(postMapper.selectById(postId));
        refreshCommunityRanklists();
        return success();
    }

    @Override
    public Map<String, String> removeComment(Integer commentId) {
        User user = currentUser();
        CommunityComment comment = commentMapper.selectById(commentId);
        if (comment == null) return error("comment not found");
        if (!comment.getUserId().equals(user.getId())) return error("permission denied");
        commentMapper.deleteById(commentId);
        communitySearchService.syncPost(postMapper.selectById(comment.getPostId()));
        refreshCommunityRanklists();
        return success();
    }

    private void refreshCommunityRanklists() {
        ranklistService.requestRefresh("community");
        ranklistService.requestRefresh("active");
    }
}
