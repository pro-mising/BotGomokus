package com.kob.backend.service.community;

import com.alibaba.fastjson2.JSONObject;

import java.util.Map;

public interface CommunityService {
    JSONObject getPostList(Integer page);
    JSONObject searchPostList(Map<String, String> data);
    JSONObject getPostDetail(Integer postId);
    Map<String, String> addPost(Map<String, String> data);
    Map<String, String> removePost(Integer postId);
    Map<String, String> likePost(Integer postId);
    Map<String, String> unlikePost(Integer postId);
    JSONObject getCommentList(Integer postId);
    Map<String, String> addComment(Map<String, String> data);
    Map<String, String> removeComment(Integer commentId);
}
