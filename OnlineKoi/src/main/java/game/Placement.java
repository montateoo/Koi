package game;

import java.util.List;

// un modo di piazzare una tessera in risposta a una domanda: dove (anchor), girata come (rotation)
// e gli esagoni che occuperebbe, cosi' il client ne mostra l'anteprima senza conoscere le forme
public record Placement(HexPosition anchor, int rotation, List<HexPosition> cells) {

    public Placement {
        cells = List.copyOf(cells);
    }
}
