package com.example.lianliankan.util;

import com.example.lianliankan.model.AnimalItem;

import java.util.LinkedList;
import java.util.List;

/**
 * 连连看核心游戏引擎
 * 功能：路径查找（0/1/2拐点）、胜负判定、死局检测
 */
public class GameEngine {

    public static final int BOARD_ROWS = 9;
    public static final int BOARD_COLS = 8;
    public static final int TOTAL_CELLS = BOARD_ROWS * BOARD_COLS;
    public static final int PAIRS_COUNT = TOTAL_CELLS / 2;

    public static final int DIFFICULTY_EASY = 0;
    public static final int DIFFICULTY_MEDIUM = 1;
    public static final int DIFFICULTY_HARD = 2;

    public static final int[] DIFFICULTY_ANIMAL_COUNTS = {10, 15, 25};

    /**
     * 将棋盘坐标映射到board列表索引
     */
    private static int indexOf(int row, int col, List<AnimalItem> board) {
        return row * BOARD_COLS + col;
    }

    /**
     * 判断指定位置是否为空。当前玩法不允许沿棋盘外框绕行。
     */
    private static boolean isEmpty(int row, int col, List<AnimalItem> board) {
        if (board == null) return false;
        if (row < 0 || row >= BOARD_ROWS || col < 0 || col >= BOARD_COLS) {
            return false;
        }
        int index = indexOf(row, col, board);
        if (index < 0 || index >= board.size() || board.get(index) == null) return false;
        return board.get(index).isMatched();
    }

    /**
     * 检查水平或垂直直线上所有中间格子是否为空
     * @param sameCoord 行号（水平线）或列号（垂直线）
     * @param start 起始坐标（列号或行号）
     * @param end 终止坐标（列号或行号）
     * @param isHorizontal true=水平线，false=垂直线
     */
    private static boolean isLineEmpty(int sameCoord, int start, int end,
                                       boolean isHorizontal, List<AnimalItem> board) {
        int min = Math.min(start, end) + 1;
        int max = Math.max(start, end);
        for (int k = min; k < max; k++) {
            if (isHorizontal) {
                if (!isEmpty(sameCoord, k, board)) return false;
            } else {
                if (!isEmpty(k, sameCoord, board)) return false;
            }
        }
        return true;
    }

    /**
     * 核心算法：判断两个格子是否可以连接
     * 路径最多2个拐点，经过的格子必须已消除，不允许沿棋盘外框绕行。
     */
    public static boolean isLinkable(AnimalItem a, AnimalItem b, List<AnimalItem> board) {
        if (a == null || b == null) return false;
        if (a.getAnimalId() != b.getAnimalId()) return false;
        if (a.isMatched() || b.isMatched()) return false;
        if (a.getRow() == b.getRow() && a.getCol() == b.getCol()) return false;

        int ra = a.getRow(), ca = a.getCol();
        int rb = b.getRow(), cb = b.getCol();

        // ---- 0个拐点：直接直线连接 ----
        if (ra == rb) {
            // 同行水平连接
            return isLineEmpty(ra, ca, cb, true, board);
        }
        if (ca == cb) {
            // 同列垂直连接
            return isLineEmpty(ca, ra, rb, false, board);
        }

        // ---- 1个拐点：L形连接 ----
        // 拐点1：(ra, cb) — A水平到拐点，拐点垂直到B
        if (isEmpty(ra, cb, board)) {
            if (isLineEmpty(ra, ca, cb, true, board)  // A水平到(ra,cb)
                    && isLineEmpty(cb, ra, rb, false, board)) { // (ra,cb)垂直到B
                return true;
            }
        }
        // 拐点2：(rb, ca) — A垂直到拐点，拐点水平到B
        if (isEmpty(rb, ca, board)) {
            if (isLineEmpty(ca, ra, rb, false, board)  // A垂直到(rb,ca)
                    && isLineEmpty(rb, ca, cb, true, board)) { // (rb,ca)水平到B
                return true;
            }
        }

        // ---- 2个拐点 ----
        // 类型1：A垂直→P1(r,ca)，P1水平→P2(r,cb)，P2垂直→B
        // 遍历棋盘内所有可能的中间行r
        for (int r = 0; r < BOARD_ROWS; r++) {
            if (!isEmpty(r, ca, board) || !isEmpty(r, cb, board)) continue;
            // 检查三段线
            boolean seg1 = (r == ra) || isLineEmpty(ca, ra, r, false, board);  // A垂直到(r,ca)
            boolean seg2 = isLineEmpty(r, ca, cb, true, board);                // (r,ca)水平到(r,cb)
            boolean seg3 = (r == rb) || isLineEmpty(cb, r, rb, false, board);  // (r,cb)垂直到B
            if (seg1 && seg2 && seg3) return true;
        }

        // 类型2：A水平→P1(ra,c)，P1垂直→P2(rb,c)，P2水平→B
        // 遍历棋盘内所有可能的中间列c
        for (int c = 0; c < BOARD_COLS; c++) {
            if (!isEmpty(ra, c, board) || !isEmpty(rb, c, board)) continue;
            boolean seg1 = (c == ca) || isLineEmpty(ra, ca, c, true, board);    // A水平到(ra,c)
            boolean seg2 = isLineEmpty(c, ra, rb, false, board);
            boolean seg3 = (c == cb) || isLineEmpty(rb, c, cb, true, board);    // (rb,c)水平到B
            if (seg1 && seg2 && seg3) return true;
        }

        return false;
    }

    /**
     * 寻找一条具体路径（用于绘制连线）
     * 返回路径点列表，按顺序从A到B
     */
    public static LinkedList<Point> findPath(AnimalItem a, AnimalItem b, List<AnimalItem> board) {
        LinkedList<Point> path = new LinkedList<>();
        if (a == null || b == null) return path;
        if (a.getAnimalId() != b.getAnimalId()) return path;
        if (a.isMatched() || b.isMatched()) return path;
        if (a.getRow() == b.getRow() && a.getCol() == b.getCol()) return path;

        int ra = a.getRow(), ca = a.getCol();
        int rb = b.getRow(), cb = b.getCol();

        // 0拐点
        if (ra == rb && isLineEmpty(ra, ca, cb, true, board)) {
            path.add(new Point(ra, ca));
            path.add(new Point(rb, cb));
            return path;
        }
        if (ca == cb && isLineEmpty(ca, ra, rb, false, board)) {
            path.add(new Point(ra, ca));
            path.add(new Point(rb, cb));
            return path;
        }

        // 1拐点
        if (isEmpty(ra, cb, board)) {
            if (isLineEmpty(ra, ca, cb, true, board) && isLineEmpty(cb, ra, rb, false, board)) {
                path.add(new Point(ra, ca));
                path.add(new Point(ra, cb));
                path.add(new Point(rb, cb));
                return path;
            }
        }
        if (isEmpty(rb, ca, board)) {
            if (isLineEmpty(ca, ra, rb, false, board) && isLineEmpty(rb, ca, cb, true, board)) {
                path.add(new Point(ra, ca));
                path.add(new Point(rb, ca));
                path.add(new Point(rb, cb));
                return path;
            }
        }

        // 2拐点 - 类型1
        for (int r = 0; r < BOARD_ROWS; r++) {
            if (!isEmpty(r, ca, board) || !isEmpty(r, cb, board)) continue;
            boolean seg1 = (r == ra) || isLineEmpty(ca, ra, r, false, board);
            boolean seg2 = isLineEmpty(r, ca, cb, true, board);
            boolean seg3 = (r == rb) || isLineEmpty(cb, r, rb, false, board);
            if (seg1 && seg2 && seg3) {
                path.add(new Point(ra, ca));
                path.add(new Point(r, ca));
                path.add(new Point(r, cb));
                path.add(new Point(rb, cb));
                return path;
            }
        }

        // 2拐点 - 类型2
        for (int c = 0; c < BOARD_COLS; c++) {
            if (!isEmpty(ra, c, board) || !isEmpty(rb, c, board)) continue;
            boolean seg1 = (c == ca) || isLineEmpty(ra, ca, c, true, board);
            boolean seg2 = isLineEmpty(c, ra, rb, false, board);
            boolean seg3 = (c == cb) || isLineEmpty(rb, c, cb, true, board);
            if (seg1 && seg2 && seg3) {
                path.add(new Point(ra, ca));
                path.add(new Point(ra, c));
                path.add(new Point(rb, c));
                path.add(new Point(rb, cb));
                return path;
            }
        }

        return path;
    }

    /**
     * 判断游戏是否胜利（所有格子均已匹配）
     */
    public static boolean isGameWon(List<AnimalItem> board) {
        if (board == null || board.isEmpty()) return false;
        for (AnimalItem item : board) {
            if (item == null) return false;
            if (!item.isMatched()) return false;
        }
        return true;
    }

    /**
     * 检测是否存在至少一对可消除的动物
     */
    public static boolean hasAnyLinkablePair(List<AnimalItem> board) {
        if (board == null || board.isEmpty()) return false;
        for (int i = 0; i < board.size(); i++) {
            AnimalItem a = board.get(i);
            if (a == null || a.isMatched()) continue;
            for (int j = i + 1; j < board.size(); j++) {
                AnimalItem b = board.get(j);
                if (b == null || b.isMatched()) continue;
                if (a.getAnimalId() == b.getAnimalId()) {
                    if (isLinkable(a, b, board)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 寻找一对可消除的配对（下标）
     */
    public static int[] findOneLinkablePair(List<AnimalItem> board) {
        if (board == null || board.isEmpty()) return null;
        for (int i = 0; i < board.size(); i++) {
            AnimalItem a = board.get(i);
            if (a == null || a.isMatched()) continue;
            for (int j = i + 1; j < board.size(); j++) {
                AnimalItem b = board.get(j);
                if (b == null || b.isMatched()) continue;
                if (a.getAnimalId() == b.getAnimalId() && isLinkable(a, b, board)) {
                    return new int[]{i, j};
                }
            }
        }
        return null;
    }

    /**
     * 路径点
     */
    public static class Point {
        public final int row, col;

        public Point(int row, int col) {
            this.row = row;
            this.col = col;
        }
    }
}
