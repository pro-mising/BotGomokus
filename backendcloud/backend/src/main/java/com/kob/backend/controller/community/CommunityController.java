package com.kob.backend.controller.community;

import com.alibaba.fastjson2.JSONObject;
import com.kob.backend.service.community.CommunityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class CommunityController {
    @Autowired
    private CommunityService communityService;

    @GetMapping("/community/post/list/")
    public JSONObject getPostList(@RequestParam Map<String, String> data) {
        Integer page = Integer.parseInt(data.getOrDefault("page", "1"));
        return communityService.getPostList(page);
    }

    @GetMapping("/community/post/search/")
    public JSONObject searchPostList(@RequestParam Map<String, String> data) {
        return communityService.searchPostList(data);
    }

    @GetMapping("/community/post/detail/")
    public JSONObject getPostDetail(@RequestParam Map<String, String> data) {
        Integer postId = Integer.parseInt(data.get("post_id"));
        return communityService.getPostDetail(postId);
    }

    @PostMapping("/community/post/add/")
    public Map<String, String> addPost(@RequestParam Map<String, String> data) {
        return communityService.addPost(data);
    }

    @PostMapping("/community/post/remove/")
    public Map<String, String> removePost(@RequestParam Map<String, String> data) {
        Integer postId = Integer.parseInt(data.get("post_id"));
        return communityService.removePost(postId);
    }

    @PostMapping("/community/post/like/")
    public Map<String, String> likePost(@RequestParam Map<String, String> data) {
        Integer postId = Integer.parseInt(data.get("post_id"));
        return communityService.likePost(postId);
    }

    @PostMapping("/community/post/unlike/")
    public Map<String, String> unlikePost(@RequestParam Map<String, String> data) {
        Integer postId = Integer.parseInt(data.get("post_id"));
        return communityService.unlikePost(postId);
    }

    @GetMapping("/community/comment/list/")
    public JSONObject getCommentList(@RequestParam Map<String, String> data) {
        Integer postId = Integer.parseInt(data.get("post_id"));
        return communityService.getCommentList(postId);
    }

    @PostMapping("/community/comment/add/")
    public Map<String, String> addComment(@RequestParam Map<String, String> data) {
        return communityService.addComment(data);
    }

    @PostMapping("/community/comment/remove/")
    public Map<String, String> removeComment(@RequestParam Map<String, String> data) {
        Integer commentId = Integer.parseInt(data.get("comment_id"));
        return communityService.removeComment(commentId);
    }
}
