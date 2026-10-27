package network;

import java.util.List;

// una risposta possibile a una domanda di piazzamento (opzione numero "option"): l'esagono da cliccare,
// la rotazione e gli esagoni che la tessera occuperebbe, per l'anteprima
public record PlacementView(int option, HexCoord anchor, int rotation, List<HexCoord> cells) {

    public PlacementView {
        cells = List.copyOf(cells);
    }
}
