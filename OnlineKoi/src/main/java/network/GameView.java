package network;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// fotografia della partita costruita dal server per un giocatore preciso (viewer)
public record GameView(
        String viewer,
        String activePlayer,
        List<String> board,
        int smallKoiLeft,
        int mediumKoiLeft,
        int bigKoiLeft,
        List<PlayerView> players,
        String question,
        List<String> options,
        Map<String, List<HexCoord>> playablePositions,
        Map<String, Map<HexCoord, List<Integer>>> rotations,
        boolean canDraw,
        List<ObjectiveView> objectives,
        List<PlacementView> placements) {


    public GameView {
        board = List.copyOf(board);
        players = List.copyOf(players);
        options = options == null ? List.of() : List.copyOf(options);
        objectives = objectives == null ? List.of() : List.copyOf(objectives);
        // la domanda aperta si risponde cliccando lo stagno: un elemento per opzione (solo per chi deve rispondere)
        placements = placements == null ? List.of() : List.copyOf(placements);

        Map<String, List<HexCoord>> copy = new LinkedHashMap<>();
        if (playablePositions != null) {
            playablePositions.forEach((type, coords) -> copy.put(type, List.copyOf(coords)));
        }
        playablePositions = Collections.unmodifiableMap(copy);

        // per le tessere su piu' esagoni (sponda, carpa media e grande):
        // tipo -> posizione di ancoraggio -> rotazioni (0-5) con cui si puo' piazzare
        Map<String, Map<HexCoord, List<Integer>>> rotationsCopy = new LinkedHashMap<>();
        if (rotations != null) {
            rotations.forEach((type, byAnchor) -> {
                Map<HexCoord, List<Integer>> anchors = new LinkedHashMap<>();
                byAnchor.forEach((anchor, values) -> anchors.put(anchor, List.copyOf(values)));
                rotationsCopy.put(type, Collections.unmodifiableMap(anchors));
            });
        }
        rotations = Collections.unmodifiableMap(rotationsCopy);
    }

    public List<HexCoord> positionsFor(String tileType) {
        return playablePositions.getOrDefault(tileType, List.of());
    }

    // rotazioni valide per quel tipo in quella posizione; vuota se il tipo non ruota o la posizione non e' valida
    public List<Integer> rotationsAt(String tileType, HexCoord anchor) {
        return rotations.getOrDefault(tileType, Map.of()).getOrDefault(anchor, List.of());
    }

    public boolean isMyTurn() {
        return viewer != null && viewer.equals(activePlayer);
    }

    // le destinazioni di una carpa gia' nello stagno stanno in playablePositions e rotations sotto la chiave "MOVE:<id>"
    private static final String MOVE_PREFIX = "MOVE:";

    public static String moveKey(int koiId) {
        return MOVE_PREFIX + koiId;
    }

    public static boolean isMoveKey(String key) {
        return key != null && key.startsWith(MOVE_PREFIX);
    }

    public static int koiIdOf(String moveKey) {
        return Integer.parseInt(moveKey.substring(MOVE_PREFIX.length()));
    }
}
