package com.example.lianliankan.util;

import com.example.lianliankan.model.AnimalItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GameGenerator {

    public static final int BOARD_ROWS = GameEngine.BOARD_ROWS;
    public static final int BOARD_COLS = GameEngine.BOARD_COLS;

    public static List<AnimalItem> generateBoard(int rows, int cols, int difficulty) {
        return generateBoard(rows, cols, difficulty, null);
    }

    public static List<AnimalItem> generateBoard(int rows, int cols, int difficulty, Long seed) {
        difficulty = clampDifficulty(difficulty);
        int totalCells = rows * cols;
        int pairsNeeded = totalCells / 2;
        int animalTypes = GameEngine.DIFFICULTY_ANIMAL_COUNTS[difficulty];

        int[] idArray = new int[totalCells];
        for (int i = 0; i < pairsNeeded; i++) {
            int animalId = i % animalTypes;
            idArray[i * 2] = animalId;
            idArray[i * 2 + 1] = animalId;
        }

        List<Integer> idList = new ArrayList<>();
        for (int id : idArray) idList.add(id);
        Random seededRandom = seed == null ? null : new Random(seed);

        for (int attempt = 0; attempt < 200; attempt++) {
            if (seededRandom == null) {
                Collections.shuffle(idList);
            } else {
                Collections.shuffle(idList, seededRandom);
            }
            List<AnimalItem> tempBoard = new ArrayList<>();
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    int idx = r * cols + c;
                    int animalId = idList.get(idx);
                    tempBoard.add(new AnimalItem(animalId, animalId, r, c));
                }
            }
            if (GameEngine.hasAnyLinkablePair(tempBoard)) {
                return tempBoard;
            }
        }

        List<AnimalItem> board = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int idx = r * cols + c;
                int animalId = idList.get(idx);
                board.add(new AnimalItem(animalId, animalId, r, c));
            }
        }
        return board;
    }

    public static int getAnimalCountForDifficulty(int difficulty) {
        return GameEngine.DIFFICULTY_ANIMAL_COUNTS[clampDifficulty(difficulty)];
    }

    /**
     * 对当前棋盘中未匹配的方块进行重排
     * @param board 当前棋盘
     * @return 重排后的棋盘，如果多次尝试仍无解则返回最后一次结果
     */
    public static List<AnimalItem> shuffleRemaining(List<AnimalItem> board) {
        if (board == null || board.isEmpty()) {
            return new ArrayList<>();
        }
        // 收集所有未匹配的方块
        List<AnimalItem> unmatched = new ArrayList<>();
        List<GameEngine.Point> positions = new ArrayList<>();
        
        for (AnimalItem item : board) {
            if (!item.isMatched()) {
                unmatched.add(item);
                positions.add(new GameEngine.Point(item.getRow(), item.getCol()));
            }
        }
        
        if (unmatched.isEmpty()) {
            return board;
        }
        
        // 提取动物ID列表
        List<Integer> animalIds = new ArrayList<>();
        for (AnimalItem item : unmatched) {
            animalIds.add(item.getAnimalId());
        }
        
        // 尝试多次洗牌直到找到有可消除配对的棋盘
        for (int attempt = 0; attempt < 200; attempt++) {
            Collections.shuffle(animalIds);
            
            // 重新分配ID到原有位置
            List<AnimalItem> newBoard = new ArrayList<>(board.size());
            int unmatchedIndex = 0;
            
            for (int i = 0; i < board.size(); i++) {
                AnimalItem original = board.get(i);
                if (original.isMatched()) {
                    newBoard.add(original);
                } else {
                    AnimalItem newItem = new AnimalItem(
                            animalIds.get(unmatchedIndex),
                            animalIds.get(unmatchedIndex),
                            original.getRow(),
                            original.getCol()
                    );
                    newItem.setMatched(false);
                    newBoard.add(newItem);
                    unmatchedIndex++;
                }
            }
            
            if (GameEngine.hasAnyLinkablePair(newBoard)) {
                return newBoard;
            }
        }
        
        // Fallback: 返回最后一次重排结果
        Collections.shuffle(animalIds);
        List<AnimalItem> fallbackBoard = new ArrayList<>(board.size());
        int unmatchedIndex = 0;
        
        for (int i = 0; i < board.size(); i++) {
            AnimalItem original = board.get(i);
            if (original.isMatched()) {
                fallbackBoard.add(original);
            } else {
                AnimalItem newItem = new AnimalItem(
                        animalIds.get(unmatchedIndex),
                        animalIds.get(unmatchedIndex),
                        original.getRow(),
                        original.getCol()
                );
                newItem.setMatched(false);
                fallbackBoard.add(newItem);
                unmatchedIndex++;
            }
        }
        
        return fallbackBoard;
    }

    private static int clampDifficulty(int difficulty) {
        if (difficulty < GameEngine.DIFFICULTY_EASY) return GameEngine.DIFFICULTY_EASY;
        if (difficulty > GameEngine.DIFFICULTY_HARD) return GameEngine.DIFFICULTY_HARD;
        return difficulty;
    }
}
