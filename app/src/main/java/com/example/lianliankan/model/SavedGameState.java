package com.example.lianliankan.model;

import com.example.lianliankan.util.GameEngine;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SavedGameState {

    public static final int VERSION = 1;

    private final String uid;
    private final int difficulty;
    private final int score;
    private final int timeRemaining;
    private final int remainingPairs;
    private final boolean paused;
    private final long updatedAt;
    private final List<Cell> cells;

    public SavedGameState(String uid, int difficulty, int score, int timeRemaining,
                          int remainingPairs, boolean paused, long updatedAt,
                          List<Cell> cells) {
        this.uid = uid == null ? "guest" : uid;
        this.difficulty = difficulty;
        this.score = score;
        this.timeRemaining = timeRemaining;
        this.remainingPairs = remainingPairs;
        this.paused = paused;
        this.updatedAt = updatedAt;
        this.cells = cells == null ? new ArrayList<>() : cells;
    }

    public String getUid() {
        return uid;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public int getScore() {
        return score;
    }

    public int getTimeRemaining() {
        return timeRemaining;
    }

    public int getRemainingPairs() {
        return remainingPairs;
    }

    public boolean isPaused() {
        return paused;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public List<Cell> getCells() {
        return cells;
    }

    public JSONObject toJsonObject() throws JSONException {
        JSONObject root = new JSONObject();
        root.put("version", VERSION);
        root.put("uid", uid);
        root.put("difficulty", difficulty);
        root.put("score", score);
        root.put("timeRemaining", timeRemaining);
        root.put("remainingPairs", remainingPairs);
        root.put("paused", paused);
        root.put("updatedAt", updatedAt);

        JSONArray array = new JSONArray();
        for (Cell cell : cells) {
            JSONObject item = new JSONObject();
            item.put("animalId", cell.animalId);
            item.put("row", cell.row);
            item.put("col", cell.col);
            item.put("matched", cell.matched);
            array.put(item);
        }
        root.put("cells", array);
        return root;
    }

    public String toJson() throws JSONException {
        return toJsonObject().toString();
    }

    public Map<String, Object> toCloudMap() {
        Map<String, Object> root = new HashMap<>();
        root.put("version", VERSION);
        root.put("uid", uid);
        root.put("difficulty", difficulty);
        root.put("score", score);
        root.put("timeRemaining", timeRemaining);
        root.put("remainingPairs", remainingPairs);
        root.put("paused", paused);
        root.put("updatedAt", updatedAt);
        List<Map<String, Object>> cloudCells = new ArrayList<>();
        for (Cell cell : cells) {
            Map<String, Object> item = new HashMap<>();
            item.put("animalId", cell.animalId);
            item.put("row", cell.row);
            item.put("col", cell.col);
            item.put("matched", cell.matched);
            cloudCells.add(item);
        }
        root.put("cells", cloudCells);
        return root;
    }

    public static SavedGameState fromJson(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        JSONArray array = root.getJSONArray("cells");
        List<Cell> cells = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            cells.add(new Cell(
                    item.getInt("animalId"),
                    item.getInt("row"),
                    item.getInt("col"),
                    item.optBoolean("matched", false)));
        }
        return new SavedGameState(
                root.optString("uid", "guest"),
                root.optInt("difficulty", GameEngine.DIFFICULTY_EASY),
                root.optInt("score", 0),
                root.optInt("timeRemaining", 120),
                root.optInt("remainingPairs", GameEngine.PAIRS_COUNT),
                root.optBoolean("paused", false),
                root.optLong("updatedAt", System.currentTimeMillis()),
                cells);
    }

    public static SavedGameState fromBoard(String uid, int difficulty, int score,
                                           int timeRemaining, int remainingPairs,
                                           boolean paused, List<AnimalItem> board) {
        List<Cell> cells = new ArrayList<>();
        if (board != null) {
            for (AnimalItem item : board) {
                cells.add(new Cell(
                        item.getAnimalId(),
                        item.getRow(),
                        item.getCol(),
                        item.isMatched()));
            }
        }
        return new SavedGameState(uid, difficulty, score, timeRemaining,
                remainingPairs, paused, System.currentTimeMillis(), cells);
    }

    public List<AnimalItem> toBoard() {
        List<AnimalItem> board = new ArrayList<>();
        for (Cell cell : cells) {
            AnimalItem item = new AnimalItem(
                    cell.animalId,
                    cell.animalId,
                    cell.row,
                    cell.col);
            item.setMatched(cell.matched);
            board.add(item);
        }
        return board;
    }

    public static class Cell {
        public final int animalId;
        public final int row;
        public final int col;
        public final boolean matched;

        public Cell(int animalId, int row, int col, boolean matched) {
            this.animalId = animalId;
            this.row = row;
            this.col = col;
            this.matched = matched;
        }
    }
}
