package com.example.lianliankan.model;

import java.util.HashMap;
import java.util.Map;

public class RankRecord {

    public String id;
    public String uid;
    public String playerName;
    public int score;
    public int timeUsed;
    public int difficulty;
    public long createdAt;
    public boolean synced;

    public RankRecord() {
    }

    public RankRecord(String id, String uid, String playerName, int score,
                      int timeUsed, int difficulty, long createdAt, boolean synced) {
        this.id = id;
        this.uid = uid;
        this.playerName = playerName;
        this.score = score;
        this.timeUsed = timeUsed;
        this.difficulty = difficulty;
        this.createdAt = createdAt;
        this.synced = synced;
    }

    public Map<String, Object> toCloudMap() {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("playerName", playerName);
        data.put("score", score);
        data.put("timeUsed", timeUsed);
        data.put("difficulty", difficulty);
        data.put("createdAt", createdAt);
        return data;
    }
}
