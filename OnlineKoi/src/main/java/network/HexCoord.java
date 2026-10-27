package network;

public record HexCoord(int q, int r) {

    @Override
    public String toString() {
        return "(" + q + "," + r + ")";
    }
}