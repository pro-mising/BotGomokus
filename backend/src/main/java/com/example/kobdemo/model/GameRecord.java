package com.example.kobdemo.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import java.util.List;

@TableName("game_records")
public class GameRecord {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String playerName;
    private String opponentName;
    private String result;
    private Integer ratingDelta;
    private String mapJson;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private List<List<Integer>> map;

    public GameRecord() {
    }

    public GameRecord(Integer id, String playerName, String opponentName, String result, Integer ratingDelta, String mapJson, LocalDateTime createdAt) {
        this.id = id;
        this.playerName = playerName;
        this.opponentName = opponentName;
        this.result = result;
        this.ratingDelta = ratingDelta;
        this.mapJson = mapJson;
        this.createdAt = createdAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getOpponentName() {
        return opponentName;
    }

    public void setOpponentName(String opponentName) {
        this.opponentName = opponentName;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Integer getRatingDelta() {
        return ratingDelta;
    }

    public void setRatingDelta(Integer ratingDelta) {
        this.ratingDelta = ratingDelta;
    }

    public String getMapJson() {
        return mapJson;
    }

    public void setMapJson(String mapJson) {
        this.mapJson = mapJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<List<Integer>> getMap() {
        return map;
    }

    public void setMap(List<List<Integer>> map) {
        this.map = map;
    }
}
