package com.kob.backend.service.impl.user.account;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kob.backend.mapper.BotEvaluationReportMapper;
import com.kob.backend.mapper.BotMapper;
import com.kob.backend.mapper.CommunityCommentMapper;
import com.kob.backend.mapper.CommunityPostMapper;
import com.kob.backend.mapper.RecordMapper;
import com.kob.backend.mapper.UserMapper;
import com.kob.backend.pojo.Bot;
import com.kob.backend.pojo.BotEvaluationReport;
import com.kob.backend.pojo.CommunityComment;
import com.kob.backend.pojo.CommunityPost;
import com.kob.backend.pojo.Record;
import com.kob.backend.pojo.User;
import com.kob.backend.service.impl.utils.RedisCacheService;
import com.kob.backend.service.impl.utils.UserDetailsImpl;
import com.kob.backend.service.user.account.InfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InfoServiceImpl implements InfoService {
    @Autowired
    private RedisCacheService redisCacheService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RecordMapper recordMapper;

    @Autowired
    private BotMapper botMapper;

    @Autowired
    private BotEvaluationReportMapper reportMapper;

    @Autowired
    private CommunityPostMapper postMapper;

    @Autowired
    private CommunityCommentMapper commentMapper;

    @Override
    public Map<String, String> getinfo() {
        User user = currentUser();
        Map<String,String> map = new HashMap<>();
        redisCacheService.markOnline(user);
        map.put("error_message", "success");
        map.put("id", user.getId().toString());
        map.put("username", user.getUsername());
        map.put("photo", user.getPhoto());
        map.put("online", isActive(user) ? "true" : "false");
        return map;
    }

    @Override
    public Map<String, String> getOnlineStatus() {
        User user = currentUser();
        Map<String, String> map = new HashMap<>();
        redisCacheService.markOnline(user);
        map.put("error_message", "success");
        map.put("online", isActive(user) ? "true" : "false");
        return map;
    }

    @Override
    public JSONObject getProfileOverview() {
        User user = currentUser();
        redisCacheService.markOnline(user);

        JSONObject resp = new JSONObject();
        resp.put("error_message", "success");
        resp.put("profile", buildProfile(user));
        resp.put("battle", buildBattleData(user));
        resp.put("bot", buildBotData(user));
        resp.put("community", buildCommunityData(user));
        return resp;
    }

    private boolean isActive(User user) {
        return user != null;
    }

    private JSONObject buildProfile(User user) {
        JSONObject profile = new JSONObject();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("photo", user.getPhoto());
        profile.put("rating", user.getRating() == null ? 0 : user.getRating());
        profile.put("online", redisCacheService.isOnline(user.getId()) || isActive(user));
        profile.put("ladder_rank", calculateLadderRank(user));
        return profile;
    }

    private JSONObject buildBattleData(User user) {
        List<Record> records = recordMapper.selectList(null);
        int games = 0;
        int wins = 0;
        int losses = 0;
        int draws = 0;
        List<JSONObject> recent = new ArrayList<>();

        records.sort(Comparator.comparing(Record::getCreatetime, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        for (Record record : records) {
            boolean isA = user.getId().equals(record.getAId());
            boolean isB = user.getId().equals(record.getBId());
            if (!isA && !isB) continue;

            games++;
            if ("all".equals(record.getLoser())) draws++;
            else if ((isA && "B".equals(record.getLoser())) || (isB && "A".equals(record.getLoser()))) wins++;
            else losses++;

            if (recent.size() < 3) {
                Integer opponentId = isA ? record.getBId() : record.getAId();
                User opponent = userMapper.selectById(opponentId);
                JSONObject item = new JSONObject();
                item.put("id", record.getId());
                item.put("opponent", opponent == null ? "Unknown" : opponent.getUsername());
                item.put("result", "all".equals(record.getLoser()) ? "平局" : (((isA && "B".equals(record.getLoser())) || (isB && "A".equals(record.getLoser()))) ? "胜利" : "失败"));
                item.put("createtime", record.getCreatetime());
                recent.add(item);
            }
        }

        JSONObject battle = new JSONObject();
        battle.put("total_games", games);
        battle.put("wins", wins);
        battle.put("losses", losses);
        battle.put("draws", draws);
        battle.put("win_rate", games == 0 ? 0 : round(wins * 100.0 / games));
        battle.put("recent_records", recent);
        return battle;
    }

    private JSONObject buildBotData(User user) {
        QueryWrapper<Bot> botQuery = new QueryWrapper<>();
        botQuery.eq("user_id", user.getId());
        List<Bot> bots = botMapper.selectList(botQuery);

        QueryWrapper<BotEvaluationReport> reportQuery = new QueryWrapper<>();
        reportQuery.eq("user_id", user.getId()).orderByDesc("createtime");
        List<BotEvaluationReport> reports = reportMapper.selectList(reportQuery);

        BotEvaluationReport latestReport = reports.isEmpty() ? null : reports.get(0);
        double bestScore = 0;
        String bestBotName = "";
        for (BotEvaluationReport report : reports) {
            JSONObject reportJson = parseJson(report.getReportJson());
            JSONObject metrics = reportJson.getJSONObject("metrics");
            if (metrics == null) continue;
            double score = metrics.getDoubleValue("overall_score");
            if (score >= bestScore) {
                bestScore = score;
                bestBotName = report.getBotName();
            }
        }

        JSONObject bot = new JSONObject();
        bot.put("bot_count", bots.size());
        bot.put("latest_bot_name", latestReport == null ? "" : latestReport.getBotName());
        bot.put("latest_mode", latestReport == null ? "" : ("standard".equals(latestReport.getMode()) ? "标准测评" : "快速测评"));
        bot.put("latest_time", latestReport == null ? null : latestReport.getCreatetime());
        bot.put("best_score", round(bestScore));
        bot.put("best_bot_name", bestBotName);
        return bot;
    }

    private JSONObject buildCommunityData(User user) {
        QueryWrapper<CommunityPost> postQuery = new QueryWrapper<>();
        postQuery.eq("user_id", user.getId());
        List<CommunityPost> posts = postMapper.selectList(postQuery);

        int likesReceived = 0;
        int commentsReceived = 0;
        for (CommunityPost post : posts) {
            likesReceived += post.getLikes() == null ? 0 : post.getLikes();
            QueryWrapper<CommunityComment> commentQuery = new QueryWrapper<>();
            commentQuery.eq("post_id", post.getId());
            commentsReceived += commentMapper.selectCount(commentQuery).intValue();
        }

        QueryWrapper<CommunityComment> madeCommentQuery = new QueryWrapper<>();
        madeCommentQuery.eq("user_id", user.getId());
        int commentsMade = commentMapper.selectCount(madeCommentQuery).intValue();
        int contributionScore = posts.size() * 20 + likesReceived * 10 + commentsMade * 5;

        JSONObject community = new JSONObject();
        community.put("posts", posts.size());
        community.put("likes_received", likesReceived);
        community.put("comments_made", commentsMade);
        community.put("comments_received", commentsReceived);
        community.put("contribution_score", contributionScore);
        return community;
    }

    private int calculateLadderRank(User user) {
        Integer rating = user.getRating() == null ? 0 : user.getRating();
        QueryWrapper<User> query = new QueryWrapper<>();
        query.gt("rating", rating);
        return userMapper.selectCount(query).intValue() + 1;
    }

    private JSONObject parseJson(String text) {
        try {
            return JSONObject.parseObject(text);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private User currentUser() {
        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl loginUser = (UserDetailsImpl) authentication.getPrincipal();
        return loginUser.getUser();
    }
}
