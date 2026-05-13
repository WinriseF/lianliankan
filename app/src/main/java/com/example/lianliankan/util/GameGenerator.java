package com.example.lianliankan.util;

import com.example.lianliankan.model.AnimalItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameGenerator {

    public static final int BOARD_ROWS = GameEngine.BOARD_ROWS;
    public static final int BOARD_COLS = GameEngine.BOARD_COLS;

    public static List<AnimalItem> generateBoard(int rows, int cols, int difficulty) {
        int totalCells = rows * cols;
        int pairsNeeded = totalCells / 2;
        int animalTypes = GameEngine.DIFFICULTY_ANIMAL_COUNTS[difficulty];

        int[] colorArray = new int[totalCells];
        for (int i = 0; i < pairsNeeded; i++) {
            int colorVal = getColorForAnimalIndex(i % animalTypes);
            colorArray[i * 2] = colorVal;
            colorArray[i * 2 + 1] = colorVal;
        }

        List<Integer> colorList = new ArrayList<>();
        for (int c : colorArray) colorList.add(c);

        // 尝试多次洗牌直到找到有可消除配对的棋盘
        for (int attempt = 0; attempt < 200; attempt++) {
            Collections.shuffle(colorList);
            List<AnimalItem> tempBoard = new ArrayList<>();
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    int idx = r * cols + c;
                    tempBoard.add(new AnimalItem(colorList.get(idx), colorList.get(idx), r, c));
                }
            }
            if (GameEngine.hasAnyLinkablePair(tempBoard)) {
                return tempBoard;
            }
        }

        // Fallback: 返回最后生成的棋盘
        List<AnimalItem> board = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int idx = r * cols + c;
                board.add(new AnimalItem(colorList.get(idx), colorList.get(idx), r, c));
            }
        }
        return board;
    }

    public static int getColorForAnimalIndex(int index) {
        int[] colors = {
                0xFFFF5252, 0xFFFF9800, 0xFFFFEB3B, 0xFF4CAF50, 0xFF2196F3,
                0xFF9C27B0, 0xFFE91E63, 0xFF00BCD4, 0xFF8BC34A, 0xFFFF5722,
                0xFF673AB7, 0xFF009688, 0xFFCDDC39, 0xFF00ACC1, 0xFF795548,
                0xFF607D8B, 0xFF3F51B5, 0xFF03A9F4, 0xFFE040FB, 0xFFFF6E40,
                0xFF18FFFF, 0xFFB2FF59, 0xFFEA80FC, 0xFF8D6E63, 0xFF78909C
        };
        return colors[index % colors.length];
    }

    public static int getAnimalCountForDifficulty(int difficulty) {
        return GameEngine.DIFFICULTY_ANIMAL_COUNTS[difficulty];
    }
}