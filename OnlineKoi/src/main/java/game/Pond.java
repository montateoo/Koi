package game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Pond {

    private static final boolean[] ALL_BLUE = {true, true, true, true, true, true};
    // sponda a rotazione 0: ali a SW (4) e SE (0) del centro, lato azzurro centrale verso S (5)
    private static final int CENTER_SIDE = 5;
    private static final int LEFT_WING = 4;
    private static final int RIGHT_WING = 0;

    // per ogni esagono occupato: tessera di base, eventuale tessera sopra, lati azzurri
    private final Map<HexPosition, Cell> cells = new HashMap<>();

    public Pond() {
        // sorgente a X: centro + i quattro vicini in diagonale (NE, NW, SE, SW); sopra e sotto il centro resta vuoto
        place(new Tile(TileType.WATERSOURCE),
                new HexPosition(0, 0),
                new HexPosition(1, -1),   // NE
                new HexPosition(-1, 0),   // NW
                new HexPosition(1, 0),    // SE
                new HexPosition(-1, 1));  // SW
    }

    // tessere tutte azzurre: sorgente, acqua, fogliame
    public void place(Tile tile, HexPosition... positions) {
        for (HexPosition position : positions) {
            cells.put(position, new Cell(tile, ALL_BLUE.clone()));
        }
    }

    public void placeBorder(Tile border, HexPosition center, int rotation) {
        cells.put(center, new Cell(border, sides(rotation, CENTER_SIDE), true));
        cells.put(center.neighbour((LEFT_WING + rotation) % 6), new Cell(border, sides(rotation, 4, 5, 0)));
        cells.put(center.neighbour((RIGHT_WING + rotation) % 6), new Cell(border, sides(rotation, 4, 5, 0)));
    }

    // lati azzurri della forma a rotazione 0, ruotati
    private static boolean[] sides(int rotation, int... blueAtZero) {
        boolean[] result = new boolean[6];
        for (int side : blueAtZero) {
            result[(side + rotation) % 6] = true;
        }
        return result;
    }

    // ninfea su fogliame, lanterna su sponda: la tessera di base resta
    public void upgrade(HexPosition position, Tile tile) {
        cells.get(position).setTop(tile);
    }

    // sola lettura: serve al server per costruire la view dello stagno
    public Map<HexPosition, Cell> getCells() {
        return Collections.unmodifiableMap(cells);
    }

    public boolean isFree(HexPosition position) {
        return !cells.containsKey(position);
    }

    public List<HexPosition> getPlayablePositions(TileType tileType) {
        Set<HexPosition> playable = new HashSet<>();

        switch (tileType) {
            case WATER:
            case LEAF:
                for (HexPosition occupied : cells.keySet()) {
                    for (HexPosition candidate : occupied.neighbours()) {
                        if (isFree(candidate) && touchesBlue(candidate)) {
                            playable.add(candidate);
                        }
                    }
                }
                break;
            case LILY:
                for (Map.Entry<HexPosition, Cell> entry : cells.entrySet()) {
                    if (entry.getValue().isFreeLeaf()) {
                        playable.add(entry.getKey());
                    }
                }
                break;
            case BORDER:
                for (HexPosition occupied : cells.keySet()) {
                    for (HexPosition candidate : occupied.neighbours()) {
                        if (!validBorderRotations(candidate).isEmpty()) {
                            playable.add(candidate);
                        }
                    }
                }
                break;
            case LANTERN:
                for (Map.Entry<HexPosition, Cell> entry : cells.entrySet()) {
                    if (entry.getValue().isFreeBorder()) {
                        playable.add(entry.getKey());
                    }
                }
                break;
            case SMALLKOI:
                for (Map.Entry<HexPosition, Cell> entry : cells.entrySet()) {
                    if (entry.getValue().isFreeWater()) {
                        playable.add(entry.getKey());
                    }
                }
                break;
            case MEDIUMKOI:
            case BIGKOI:
                // l'ancoraggio e' un'acqua libera da cui la carpa entra con almeno una rotazione
                for (Map.Entry<HexPosition, Cell> entry : cells.entrySet()) {
                    if (entry.getValue().isFreeWater() && !validRotations(tileType, entry.getKey()).isEmpty()) {
                        playable.add(entry.getKey());
                    }
                }
                break;
            default:
                break;
        }

        List<HexPosition> sorted = new ArrayList<>(playable);
        sorted.sort(Comparator.comparingInt(HexPosition::r).thenComparingInt(HexPosition::q));
        return sorted;
    }

    public boolean canPlaceBorder(HexPosition center, int rotation) {
        HexPosition left = center.neighbour((LEFT_WING + rotation) % 6);
        HexPosition right = center.neighbour((RIGHT_WING + rotation) % 6);
        if (!isFree(center) || !isFree(left) || !isFree(right)) {
            return false;
        }
        int side = (CENTER_SIDE + rotation) % 6;
        Cell facing = cells.get(center.neighbour(side));
        return facing != null && facing.isBlue((side + 3) % 6);
    }

    // celle occupate da una carpa a rotazione 0, come direzioni a partire dall'esagono di ancoraggio
    private static int[] koiShape(TileType koi) {
        switch (koi) {
            case SMALLKOI:  return new int[] {};          // solo l'ancoraggio
            case MEDIUMKOI: return new int[] {0};         // ancoraggio + un vicino
            case BIGKOI:    return new int[] {0, 1};      // ancoraggio + due vicini adiacenti tra loro (triangolo)
            default: throw new IllegalArgumentException(koi + " is not a koi");
        }
    }

    private List<HexPosition> koiCells(TileType koi, HexPosition anchor, int rotation) {
        List<HexPosition> result = new ArrayList<>();
        result.add(anchor);
        for (int direction : koiShape(koi)) {
            result.add(anchor.neighbour((direction + rotation) % 6));
        }
        return result;
    }

    public boolean canPlaceKoi(TileType koi, HexPosition anchor, int rotation) {
        for (HexPosition position : koiCells(koi, anchor, rotation)) {
            Cell cell = cells.get(position);
            if (cell == null || !cell.isFreeWater()) {
                return false;
            }
        }
        return true;
    }

    public void placeKoi(Tile koi, HexPosition anchor, int rotation) {
        for (HexPosition position : koiCells(koi.getTileType(), anchor, rotation)) {
            cells.get(position).setTop(koi, rotation);
        }
    }


    public List<Integer> validBorderRotations(HexPosition center) {
        return validRotations(TileType.BORDER, center);
    }

    // rotazioni (0-5) con cui una tessera su piu' esagoni entra in questa posizione: sponda o carpa
    public List<Integer> validRotations(TileType tileType, HexPosition anchor) {
        List<Integer> rotations = new ArrayList<>();
        for (int rotation = 0; rotation < 6; rotation++) {
            boolean fits = tileType == TileType.BORDER
                    ? canPlaceBorder(anchor, rotation)
                    : canPlaceKoi(tileType, anchor, rotation);
            if (fits) {
                rotations.add(rotation);
            }
        }
        return rotations;
    }

    // P e' adiacente a un lato azzurro se un vicino, nella direzione d, ha azzurro sul lato che guarda P
    private boolean touchesBlue(HexPosition p) {
        for (int d = 0; d < 6; d++) {
            Cell neighbour = cells.get(p.neighbour(d));
            if (neighbour != null && neighbour.isBlue((d + 3) % 6)) {
                return true;
            }
        }
        return false;
    }

}

