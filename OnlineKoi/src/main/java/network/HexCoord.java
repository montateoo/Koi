package network;

import java.io.Serializable;

public record HexCoord(int q, int r) implements Serializable {

    @Override
    public String toString() {
        return "(" + q + "," + r + ")";
    }
}