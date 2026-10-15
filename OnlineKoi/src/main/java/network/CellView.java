package network;

import java.io.Serializable;
import java.util.List;

// un esagono occupato dello stagno, cosi' come lo vede il client.
// top e' null se non c'e' niente sopra; tileId uguale = stessa tessera di base (sponda, sorgente...);
// topId uguale = stessa tessera sopra (una carpa media o grande copre piu' esagoni), -1 se non c'e' niente sopra;
// blueSides ha 6 valori, nello stesso ordine delle direzioni di HexPosition (0 = SE, 1 = NE, 2 = N, 3 = NW, 4 = SW, 5 = S)
public record CellView(
        int q,
        int r,
        String base,
        String top,
        int topRotation,
        int tileId,
        int topId,
        List<Boolean> blueSides) implements Serializable {

    public CellView {
        blueSides = List.copyOf(blueSides);
    }
}
