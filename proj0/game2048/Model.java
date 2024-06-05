package game2048;

import java.util.Formatter;
import java.util.Observable;


/** The state of a game of 2048.
 *  @author Kiro
 */
public class Model extends Observable {
    /** Current contents of the board. */
    private Board board;
    /** Current score. */
    private int score;
    /** Maximum score so far.  Updated when game ends. */
    private int maxScore;
    /** True iff game is ended. */
    private boolean gameOver;

    /* Coordinate System: column C, row R of the board (where row 0,
     * column 0 is the lower-left corner of the board) will correspond
     * to board.tile(c, r).  Be careful! It works like (x, y) coordinates.
     * 这么一说清晰多了，以列行而不是行列，这样就和坐标系x，y一致了，倒也方便
     */

    /** Largest piece value. */
    public static final int MAX_PIECE = 2048;

    /** A new 2048 game on a board of size SIZE with no pieces
     *  and score 0. */
    public Model(int size) {
        board = new Board(size);
        score = maxScore = 0;
        gameOver = false;
    }

    /** A new 2048 game where RAWVALUES contain the values of the tiles
     * (0 if null). VALUES is indexed by (row, col) with (0, 0) corresponding
     * to the bottom-left corner. Used for testing purposes. */
    public Model(int[][] rawValues, int score, int maxScore, boolean gameOver) {
        int size = rawValues.length;
        board = new Board(rawValues, score);
        this.score = score;
        this.maxScore = maxScore;
        this.gameOver = gameOver;
    }

    /** Return the current Tile at (COL, ROW), where 0 <= ROW < size(),
     *  0 <= COL < size(). Returns null if there is no tile there.
     *  Used for testing. Should be deprecated and removed.
     *  */
    public Tile tile(int col, int row) {
        return board.tile(col, row);
    }

    /** Return the number of squares on one side of the board.
     *  Used for testing. Should be deprecated and removed. */
    public int size() {
        return board.size();
    }

    /** Checks if the game is over and sets the gameOver variable
     *  appropriately.
     */
    private void checkGameOver() {
        gameOver = checkGameOver(board);
    }

    /** Determine whether game is over. */
    private static boolean checkGameOver(Board b) {
        return maxTileExists(b) || !atLeastOneMoveExists(b);
    }

    /**
     * Returns true if any tile is equal to the maximum valid value.
     * Maximum valid value is given by MAX_PIECE. Note that
     * given a Tile object t, we get its value with t.value().
     */
    public static boolean maxTileExists(Board b) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                Tile t = b.tile(i, j);
                if (t != null && t.value() == MAX_PIECE) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Returns true if at least one space on the Board is empty.
     *  Empty spaces are stored as null.
     *  有空就返回true，没有空就返回false
     * */
    public static boolean emptySpaceExists(Board b) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (b.tile(i, j) == null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if there are any valid moves on the board.
     * There are two ways that there can be valid moves:
     * 1. There is at least one empty space on the board.
     * 2. There are two adjacent tiles with the same value.
     */
    public static boolean atLeastOneMoveExists(Board b) {
        if (emptySpaceExists(b)) {
            return true;
        }
        // 妙啊，看别人的思路，还可以bfs，只要有相同的就返回true
        // 横向下比较
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 3; j++) {
                Tile t1 = b.tile(i, j);
                Tile t2 = b.tile(i, j + 1);
                if (t1.value() == t2.value()) {
                    return true;
                }
            }
        }
        // 纵向右比较
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 4; j++) {
                Tile t1 = b.tile(i, j);
                Tile t2 = b.tile(i + 1, j);
                if (t1.value() == t2.value()) {
                    return true;
                }
            }
        }
        return false;
    }


    /** Return true iff the game is over (there are no moves, or
     *  there is a tile with value 2048 on the board). */
    public boolean gameOver() {
        checkGameOver();
        if (gameOver) {
            maxScore = Math.max(score, maxScore);
        }
        return gameOver;
    }

    /** Return the current score. */
    public int score() {
        return score;
    }

    /** Return the current maximum game score (updated at end of game). */
    public int maxScore() {
        return maxScore;
    }

    /** Clear the board to empty and reset the score. */
    public void clear() {
        score = 0;
        gameOver = false;
        board.clear();
        setChanged();
    }

    /** Add TILE to the board. There must be no Tile currently at the
     *  same position. */
    public void addTile(Tile tile) {
        board.addTile(tile);
        checkGameOver();
        setChanged();
    }

    /** Tilt the board toward SIDE. Return true iff this changes the board.
     *
     * 1. If two Tile objects are adjacent in the direction of motion and have
     *    the same value, they are merged into one Tile of twice the original
     *    value and that new value is added to the score instance variable
     * 2. A tile that is the result of a merge will not merge again on that
     *    tilt. So each move, every tile will only ever be part of at most one
     *    merge (perhaps zero).
     * 3. When three adjacent tiles in the direction of motion have the same
     *    value, then the leading two tiles in the direction of motion merge,
     *    and the trailing tile does not.
     * */
    public boolean tilt(Side side) {
        boolean changed = false;

        board.setViewingPerspective(side);

        // 纵向向右遍历棋盘
        for (int col = 0; col < board.size(); col++) {
            // 目前可移动的最高处
            int top = 3;
            // prev是否可以被合并
            boolean merge = true;
            // 前一个tile
            Tile prev = null;
            int prevRow = 0;
            // 横向向下遍历棋盘
            for (int row = board.size() - 1; row >= 0; row--) {
                // 获取当前位置的tile
                Tile tile = board.tile(col, row);
                if (tile == null) {
                    continue;
                }
                // 如果前一个tile为空，说明当前tile是第一个tile，直接移动到top
                if (prev == null) {
                    // 移动到top
                    if (board.move(col, top, tile)) {
                        changed = true;
                    }
                    if (top != row) {
                        changed = true;
                    }
                    // 更新prev
                    prev = board.tile(col, top);
                    prevRow = top;
                    top--;
                } else {
                    // 前一个tile不为空，考虑能否合并
                    if (tile.value() == prev.value() && merge) {
                        board.move(col, prevRow, tile);

                        // 更新score
                        score += tile.value() * 2;
                        // 合并后不能再合并
                        merge = false;
                        // 更新prev
                        prev = board.tile(col, prevRow);
                        prevRow = top;
                        changed = true;
                    } else {
                        // 不能合并，直接移动到top
                        board.move(col, top, tile);
                        // 未合并可以被后续合并
                        merge = true;
                        // 更新prev
                        prev = board.tile(col, top);
                        top--;
                        changed = true;
                    }
                }
            }
        }
        board.setViewingPerspective(Side.NORTH);

        checkGameOver();
        return changed;
    }

    @Override
     /** Returns the model as a string, used for debugging. */
    public String toString() {
        Formatter out = new Formatter();
        out.format("%n[%n");
        for (int row = size() - 1; row >= 0; row -= 1) {
            for (int col = 0; col < size(); col += 1) {
                if (tile(col, row) == null) {
                    out.format("|    ");
                } else {
                    out.format("|%4d", tile(col, row).value());
                }
            }
            out.format("|%n");
        }
        String over = gameOver() ? "over" : "not over";
        out.format("] %d (max: %d) (game is %s) %n", score(), maxScore(), over);
        return out.toString();
    }

    @Override
    /** Returns whether two models are equal. */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (getClass() != o.getClass()) {
            return false;
        } else {
            return toString().equals(o.toString());
        }
    }

    @Override
    /** Returns hash code of Model’s string. */
    public int hashCode() {
        return toString().hashCode();
    }
}
