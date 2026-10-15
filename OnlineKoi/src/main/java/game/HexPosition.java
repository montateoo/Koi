package game;

import java.util.ArrayList;
import java.util.List;

public record HexPosition(int q, int r) {

    private static final int[][] DIRECTIONS = {{1, 0}, {1, -1}, {0, -1}, {-1, 0}, {-1, 1}, {0, 1}};

    public List<HexPosition> neighbours() {
        List<HexPosition> result = new ArrayList<>();
        for (int[] d : DIRECTIONS) {
            result.add(new HexPosition(q + d[0], r + d[1]));
        }
        return result;
    }

    public HexPosition neighbour(int direction) {
        int[] d = DIRECTIONS[direction];
        return new HexPosition(q + d[0], r + d[1]);
    }


    @Override
    public String toString() {
        return "(" + q + "," + r + ")";
    }

}
