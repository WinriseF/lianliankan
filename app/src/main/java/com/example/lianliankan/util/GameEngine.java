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

    // 当前资源里有 0~24 共 25 张图，困难模式保留 25 种。
    public static final int[] DIFFICULTY_ANIMAL_COUNTS = {10, 15, 25};

    /**
     * 将棋盘坐标映射到 board 列表索引。
     */
    private static int indexOf(int row, int col) {
        return row * BOARD_COLS + col;
    }

    /**
     * 判断指定位置是否为空。
     *
     * 连连看规则允许路径沿棋盘外边框绕行，所以 row/col 落在棋盘外一圈时，
     * 也视为空格。真实棋盘内的格子只有已经消除后才算空。
     */
    private static boolean isEmpty(int row, int col, List<AnimalItem> board) {
        if (board == null) return false;
        if (row < 0 || row >= BOARD_ROWS || col < 0 || col >= BOARD_COLS) {
            return true;
        }
        int index = indexOf(row, col);
        if (index < 0 || index >= board.size() || board.get(index) == null) return false;
        return board.get(index).isMatched();
    }

    /**
     * 检查水平或垂直直线上所有中间格子是否为空。
     *
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

    private static boolean isSameTile(AnimalItem a, AnimalItem b) {
        return a.getRow() == b.getRow() && a.getCol() == b.getCol();
    }

    private static boolean canTryLink(AnimalItem a, AnimalItem b, List<AnimalItem> board) {
        if (a == null || b == null) return false;
        if (board == null || board.isEmpty()) return false;
        if (a.getAnimalId() != b.getAnimalId()) return false;
        if (a.isMatched() || b.isMatched()) return false;
        return !isSameTile(a, b);
    }

    /**
     * 核心算法：判断两个格子是否可以连接。
     * 路径最多 2 个拐点，经过的真实棋盘格必须已经消除，允许沿棋盘外框绕行。
     */
    public static boolean isLinkable(AnimalItem a, AnimalItem b, List<AnimalItem> board) {
        return !findPath(a, b, board).isEmpty();
    }

    /**
     * 寻找一条具体路径（用于绘制连线）。
     * 返回路径点列表，按顺序从 A 到 B。
     */
    public static LinkedList<Point> findPath(AnimalItem a, AnimalItem b, List<AnimalItem> board) {
        LinkedList<Point> path = new LinkedList<>();
        if (!canTryLink(a, b, board)) return path;

        int ra = a.getRow(), ca = a.getCol();
        int rb = b.getRow(), cb = b.getCol();

        // 0 拐点：同一行或同一列直接连接。
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

        // 1 拐点：两个可能的 L 形拐点都在棋盘内。
        if (isEmpty(ra, cb, board)
                && isLineEmpty(ra, ca, cb, true, board)
                && isLineEmpty(cb, ra, rb, false, board)) {
            path.add(new Point(ra, ca));
            path.add(new Point(ra, cb));
            path.add(new Point(rb, cb));
            return path;
        }
        if (isEmpty(rb, ca, board)
                && isLineEmpty(ca, ra, rb, false, board)
                && isLineEmpty(rb, ca, cb, true, board)) {
            path.add(new Point(ra, ca));
            path.add(new Point(rb, ca));
            path.add(new Point(rb, cb));
            return path;
        }

        // 2 拐点：扫描中间行。范围包含 -1 和 BOARD_ROWS，代表棋盘上下外边框。
        for (int r = -1; r <= BOARD_ROWS; r++) {
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

        // 2 拐点：扫描中间列。范围包含 -1 和 BOARD_COLS，代表棋盘左右外边框。
        for (int c = -1; c <= BOARD_COLS; c++) {
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
     * 判断游戏是否胜利（所有格子均已匹配）。
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
     * 检测是否存在至少一对可消除的动物。
     */
    public static boolean hasAnyLinkablePair(List<AnimalItem> board) {
        if (board == null || board.isEmpty()) return false;
        for (int i = 0; i < board.size(); i++) {
            AnimalItem a = board.get(i);
            if (a == null || a.isMatched()) continue;
            for (int j = i + 1; j < board.size(); j++) {
                AnimalItem b = board.get(j);
                if (b == null || b.isMatched()) continue;
                if (a.getAnimalId() == b.getAnimalId() && isLinkable(a, b, board)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 寻找一对可消除的配对（下标）。
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
     * 路径点。
     */
    public static class Point {
        public final int row, col;

        public Point(int row, int col) {
            this.row = row;
            this.col = col;
        }
    }
}
