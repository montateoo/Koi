package game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import game.objectives.ObjectiveShapes;

public class Pond {

    private static final boolean[] ALL_BLUE = {true, true, true, true, true, true};
    // sponda a rotazione 0: ali a SW (4) e SE (0) del centro, lato azzurro centrale verso S (5)
    private static final int CENTER_SIDE = 5;
    private static final int LEFT_WING = 4;
    private static final int RIGHT_WING = 0;

    // per ogni esagono occupato: tessera di base, eventuale tessera sopra, lati azzurri
    private final Map<HexPosition, Cell> cells = new HashMap<>();
    // tessere Percorso in ordine, da un capo all'altro: il percorso cresce solo dai due capi
    private final List<HexPosition> path = new ArrayList<>();

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
        placeBorder(border, center, rotation, true);
    }

    public void placeBorder(Tile border, HexPosition center, int rotation, boolean lanternSpot) {
        cells.put(center, new Cell(border, sides(rotation, CENTER_SIDE), lanternSpot));
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
            case STATUE:
                for (HexPosition occupied : cells.keySet()) {
                    for (HexPosition candidate : occupied.neighbours()) {
                        if (!validBorderRotations(candidate).isEmpty()) {
                            playable.add(candidate);
                        }
                    }
                }
                break;
            case BRIDGE:
                for (HexPosition occupied : cells.keySet()) {
                    for (HexPosition candidate : occupied.neighbours()) {
                        if (!validRotations(TileType.BRIDGE, candidate).isEmpty()) {
                            playable.add(candidate);
                        }
                    }
                }
                break;
            case WATERFALL:
                for (HexPosition occupied : cells.keySet()) {
                    for (HexPosition candidate : occupied.neighbours()) {
                        if (!validRotations(TileType.WATERFALL, candidate).isEmpty()) {
                            playable.add(candidate);
                        }
                    }
                }
                break;
            case THREE: {
                Set<HexPosition> candidates = new HashSet<>(cells.keySet());
                for (int step = 0; step < 3; step++) {
                    Set<HexPosition> wider = new HashSet<>(candidates);
                    for (HexPosition position : candidates) {
                        wider.addAll(position.neighbours());
                    }
                    candidates = wider;
                }
                for (HexPosition candidate : candidates) {
                    if (isFree(candidate) && !validRotations(TileType.THREE, candidate).isEmpty()) {
                        playable.add(candidate);
                    }
                }
                break;
            }
            case PATH:
                // la prima tessera va su qualsiasi acqua libera, le altre su un'acqua libera accanto a un capo
                for (Map.Entry<HexPosition, Cell> entry : cells.entrySet()) {
                    if (entry.getValue().isFreeWater() && (path.isEmpty() || nextToPathEnd(entry.getKey()))) {
                        playable.add(entry.getKey());
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
            case SMALLBRANCH:
            case MEDIUMBRANCH:
            case BIGBRANCH:
            case SHISHIODOSHI:
            case OLDCARP:
            case TURTLE:
            case PURPLELILY:
                // obiettivi che vanno su acqua libera con una forma: almeno un orientamento deve entrare
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

    // forma della tessera obiettivo: stesse forme dei requisiti (il ramo va sopra la sua figura d'acqua)
    private static int[][] objectiveShape(TileType tileType) {
        switch (tileType) {
            case SMALLBRANCH:  return ObjectiveShapes.SMALL_BRANCH;
            case MEDIUMBRANCH: return ObjectiveShapes.MEDIUM_BRANCH;
            case BIGBRANCH:    return ObjectiveShapes.BIG_BRANCH;
            case SHISHIODOSHI: return ObjectiveShapes.SHISHI_ODOSHI;
            case OLDCARP:      return ObjectiveShapes.OLD_CARP;
            default:           return new int[][] {{}};   // tessere da un esagono: tartaruga, ninfea viola, percorso...
        }
    }

    private List<HexPosition> objectiveCells(TileType tileType, HexPosition anchor, int rotation, boolean mirrored) {
        return shapeCells(objectiveShape(tileType), anchor, rotation, mirrored);
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

    // la sorgente non vale per lo Shishi odoshi e per i rami (vanno sopra la figura del requisito)
    private static boolean sourceAllowed(TileType tileType) {
        switch (tileType) {
            case SHISHIODOSHI:
            case SMALLBRANCH:
            case MEDIUMBRANCH:
            case BIGBRANCH:
                return false;
            default:
                return true;
        }
    }

    public boolean canPlaceObjective(TileType tileType, HexPosition anchor, int rotation, boolean mirrored) {
        return allFreeWater(objectiveCells(tileType, anchor, rotation, mirrored), sourceAllowed(tileType));
    }

    // la tessera obiettivo copre tutti gli esagoni della sua forma; rotation 0-11 come in validRotations
    public void placeObjective(Tile tile, HexPosition anchor, int rotation) {
        if (!tile.getTileType().isTopTile()) {
            if(tile.getTileType() == TileType.THREE){
                placePiece(tile, anchor, rotation);
                return;
            } else {
                placeBorder(tile, anchor, rotation, false);   // pezzi tipo sponda: niente posto per la lanterna
                return;
            }
        }
        for (HexPosition position : objectiveCells(tile.getTileType(), anchor, rotation % 6, rotation >= 6)) {
            cells.get(position).setTop(tile, rotation);
        }
    }


    public void placeKoi(Tile koi, HexPosition anchor, int rotation) {
        for (HexPosition position : koiCells(koi.getTileType(), anchor, rotation)) {
            cells.get(position).setTop(koi, rotation);
        }
    }


    public List<Integer> validBorderRotations(HexPosition center) {
        return validRotations(TileType.BORDER, center);
    }

    // rotazioni con cui una tessera su piu' esagoni entra in questa posizione:
    // sponda e carpe 0-5; obiettivi con una forma 0-5 normali e 6-11 specchiati; obiettivi da un esagono solo 0
    public List<Integer> validRotations(TileType tileType, HexPosition anchor) {
        List<Integer> rotations = new ArrayList<>();
        int count = 6;
        if (tileType.isObjective() && tileType.isTopTile()) {
            count = objectiveShape(tileType).length > 1 ? 12 : 1;
        }
        Set<Set<HexPosition>> covered = new HashSet<>();   // forme simmetriche: la specchiata puo' coprire gli stessi esagoni
        for (int rotation = 0; rotation < count; rotation++) {
            boolean fits;
            if (tileType == TileType.BORDER || tileType == TileType.STATUE) {
                fits = canPlaceBorder(anchor, rotation);
            } else if (tileType == TileType.BRIDGE) {
                fits = canPlaceBorder(anchor, rotation) && nextToLitBorder(borderCells(anchor, rotation));
            } else if (tileType == TileType.WATERFALL) {
                fits = canPlaceBorder(anchor, rotation) && centerFacesSource(anchor, rotation);
            } else if (tileType.isKoi()) {
                fits = canPlaceKoi(tileType, anchor, rotation);
            } else if(tileType == TileType.THREE) {
                fits = canPlacePiece(tileType, anchor, rotation);
            } else {
                fits = canPlaceObjective(tileType, anchor, rotation % 6, rotation >= 6)
                        && covered.add(new HashSet<>(objectiveCells(tileType, anchor, rotation % 6, rotation >= 6)));
            }
            if (fits) {
                rotations.add(rotation);
            }
        }
        return rotations;
    }

    // il lato azzurro centrale guarda proprio la sorgente (che sia azzurro lo controlla gia' canPlaceBorder)
    private boolean centerFacesSource(HexPosition center, int rotation) {
        Cell facing = cells.get(center.neighbour((CENTER_SIDE + rotation) % 6));
        return facing != null && facing.getBase().getTileType() == TileType.WATERSOURCE;
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

    // i tre esagoni di un pezzo a forma di sponda: centro e due ali
    private List<HexPosition> borderCells(HexPosition center, int rotation) {
        return List.of(center,
                center.neighbour((LEFT_WING + rotation) % 6),
                center.neighbour((RIGHT_WING + rotation) % 6));
    }

    // almeno un esagono del pezzo tocca una sponda che ha la lanterna (tutta la sponda, non solo il centro)
    private boolean nextToLitBorder(List<HexPosition> piece) {
        Set<Integer> litBorders = new HashSet<>();
        for (Cell cell : cells.values()) {
            if (cell.getTop() != null && cell.getTop().getTileType() == TileType.LANTERN) {
                litBorders.add(cell.getBase().getTileID());
            }
        }
        for (HexPosition position : piece) {
            for (HexPosition next : position.neighbours()) {
                Cell cell = cells.get(next);
                if (cell != null && litBorders.contains(cell.getBase().getTileID())) {
                    return true;
                }
            }
        }
        return false;
    }

	public int countKoi() {
        Set<Integer> koi = new HashSet<>();

        for(Cell c: cells.values()) {
            if (c.getTop() != null && c.getTop().getTileType().isKoi()) {
                koi.add(c.getTop().getTileID());
            }
        }

		return koi.size();
	}

    public int countTops(TileType type) {
        Set<Integer> tops = new HashSet<>();

        for(Cell c: cells.values()) {
            if (c.getTop() != null && c.getTop().getTileType() == type) {
                tops.add(c.getTop().getTileID());
            }
        }
        
        return tops.size();
    }

    public int countFreeWater() {
        int count = 0;
        for (Map.Entry<HexPosition, Cell> entry : cells.entrySet()) {
            if (entry.getValue().isFreeWater()) {
                count ++;
            }
        }
        return count;
    }

    // due ninfee una accanto all'altra
    public boolean hasAdjacentTops(TileType type) {
        for (Map.Entry<HexPosition, Cell> entry : cells.entrySet()) {
            if (entry.getValue().getTop() == null || entry.getValue().getTop().getTileType() != type) {
                continue;
            }
            for (HexPosition next : entry.getKey().neighbours()) {
                Cell other = cells.get(next);
                if (other != null && other.getTop() != null && other.getTop().getTileType() == type) {
                    return true;
                }
            }
        }
        return false;
    }

    // esagoni della forma: si parte dall'ancoraggio e si seguono le direzioni, ruotate ed eventualmente specchiate
    private static List<HexPosition> shapeCells(int[][] shape, HexPosition anchor, int rotation, boolean mirrored) {
        List<HexPosition> result = new ArrayList<>();
        for (int[] path : shape) {
            HexPosition position = anchor;
            for (int direction : path) {
                int d = mirrored ? (6 - direction) % 6 : direction;
                position = position.neighbour((d + rotation) % 6);
            }
            result.add(position);
        }
        return result;
    }

    // prima collocazione della forma tutta su acqua libera; lista vuota se non c'e'
    public List<HexPosition> findShape(int[][] shape, boolean sourceAllowed) {
        for (HexPosition anchor : cells.keySet()) {
            for (int rotation = 0; rotation < 6; rotation++) {
                for (boolean mirrored : new boolean[] {false, true}) {
                    List<HexPosition> found = shapeCells(shape, anchor, rotation, mirrored);
                    if (allFreeWater(found, sourceAllowed)) {
                        return found;
                    }
                }
            }
        }
        return List.of();
    }

    private boolean allFreeWater(List<HexPosition> positions, boolean sourceAllowed) {
        for (HexPosition position : positions) {
            Cell cell = cells.get(position);
            if (cell == null || !cell.isFreeWater()) {
                return false;
            }
            if (!sourceAllowed && cell.getBase().getTileType() == TileType.WATERSOURCE) {
                return false;
            }
        }
        return true;
    }

    // tessere di base di un tipo (il fogliame conta anche se ha sopra una ninfea)
    public int countBase(TileType type) {
        Set<Integer> tiles = new HashSet<>();
        for (Cell c : cells.values()) {
            if (c.getBase().getTileType() == type) {
                tiles.add(c.getBase().getTileID());
            }
        }
        return tiles.size();
    }

    // esagoni coperti da una carpa con questo id; vuoto se nel laghetto non c'e'
    public List<HexPosition> findKoi(int koiId) {
        List<HexPosition> found = new ArrayList<>();
        for (Map.Entry<HexPosition, Cell> entry : cells.entrySet()) {
            Tile top = entry.getValue().getTop();
            if (top != null && top.getTileType().isKoi() && top.getTileID() == koiId) {
                found.add(entry.getKey());
            }
        }
        return found;
    }

    // la carpa si alza, si prova la nuova posizione (puo' riusare gli esagoni che lascia libera), altrimenti torna dov'era
    public boolean moveKoi(int koiId, HexPosition anchor, int rotation) {
        List<HexPosition> old = findKoi(koiId);
        if (old.isEmpty()) {
            return false;
        }
        Tile koi = cells.get(old.get(0)).getTop();
        int oldRotation = cells.get(old.get(0)).getTopRotation();
        for (HexPosition position : old) {
            cells.get(position).setTop(null, 0);
        }
        if (!canPlaceKoi(koi.getTileType(), anchor, rotation)) {
            for (HexPosition position : old) {
                cells.get(position).setTop(koi, oldRotation);
            }
            return false;
        }
        placeKoi(koi, anchor, rotation);
        return true;
    }

    // come canPlaceKoi, ma gli esagoni occupati da questa stessa carpa contano come liberi
    public boolean canMoveKoi(int koiId, HexPosition anchor, int rotation) {
        List<HexPosition> old = findKoi(koiId);
        if (old.isEmpty()) {
            return false;
        }
        TileType type = cells.get(old.get(0)).getTop().getTileType();
        for (HexPosition position : koiCells(type, anchor, rotation)) {
            Cell cell = cells.get(position);
            if (cell == null) {
                return false;
            }
            boolean itself = cell.getTop() != null && cell.getTop().getTileID() == koiId;
            if (!cell.isFreeWater() && !itself) {
                return false;
            }
        }
        return true;
    }

    private boolean nextToPathEnd(HexPosition position) {
        List<HexPosition> neighbours = position.neighbours();
        return neighbours.contains(path.get(0)) || neighbours.contains(path.get(path.size() - 1));
    }

    // la tessera va sopra l'acqua libera e allunga il percorso dal capo a cui e' attaccata
    public void placePath(Tile tile, HexPosition position) {
        cells.get(position).setTop(tile);
        if (!path.isEmpty() && position.neighbours().contains(path.get(0))
                && !position.neighbours().contains(path.get(path.size() - 1))) {
            path.add(0, position);
        } else {
            path.add(position);
        }
    }

    public int pathLength() {
        return path.size();
    }

    // la carpa coprirebbe esattamente gli esagoni che copre gia': non e' uno spostamento
    public boolean isKoiAt(int koiId, HexPosition anchor, int rotation) {
        List<HexPosition> old = findKoi(koiId);
        if (old.isEmpty()) {
            return false;
        }
        TileType type = cells.get(old.get(0)).getTop().getTileType();
        return new HashSet<>(koiCells(type, anchor, rotation)).equals(new HashSet<>(old));
    }

    // id delle carpe nello stagno (una carpa grande copre tre esagoni ma ha un solo id)
    public Set<Integer> koiIds() {
        Set<Integer> ids = new TreeSet<>();
        for (Cell cell : cells.values()) {
            if (cell.getTop() != null && cell.getTop().getTileType().isKoi()) {
                ids.add(cell.getTop().getTileID());
            }
        }
        return ids;
    }

    // dove puo' andare questa carpa: ancoraggio -> rotazioni valide (la carpa piccola usa solo la rotazione 0)
    public Map<HexPosition, List<Integer>> koiMoves(int koiId) {
        Map<HexPosition, List<Integer>> moves = new LinkedHashMap<>();
        List<HexPosition> old = findKoi(koiId);
        if (old.isEmpty()) {
            return moves;
        }
        int maxRotation = cells.get(old.get(0)).getTop().getTileType() == TileType.SMALLKOI ? 1 : 6;
        List<HexPosition> anchors = new ArrayList<>(cells.keySet());
        anchors.sort(Comparator.comparingInt(HexPosition::r).thenComparingInt(HexPosition::q));
        for (HexPosition anchor : anchors) {
            List<Integer> rotations = new ArrayList<>();
            for (int rotation = 0; rotation < maxRotation; rotation++) {
                if (canMoveKoi(koiId, anchor, rotation) && !isKoiAt(koiId, anchor, rotation)) {
                    rotations.add(rotation);
                }
            }
            if (!rotations.isEmpty()) {
                moves.put(anchor, rotations);
            }
        }
        return moves;
    }

    // pezzo nuovo di laghetto (forma e lati azzurri da TileType): tutti i suoi esagoni sono liberi
    // e almeno uno dei suoi lati azzurri tocca un lato azzurro di un'altra tessera
    public boolean canPlacePiece(TileType type, HexPosition anchor, int rotation) {
        List<HexPosition> piece = shapeCells(type.footprint(), anchor, rotation, false);
        for (HexPosition position : piece) {
            if (!isFree(position)) {
                return false;
            }
        }
        int[][] blue = type.blueSides();
        for (int i = 0; i < piece.size(); i++) {
            for (int side : blue[i]) {
                int d = (side + rotation) % 6;
                Cell neighbour = cells.get(piece.get(i).neighbour(d));
                if (neighbour != null && neighbour.isBlue((d + 3) % 6)) {
                    return true;
                }
            }
        }
        return false;
    }

    // crea gli esagoni del pezzo, ognuno con i suoi lati azzurri ruotati
    public void placePiece(Tile tile, HexPosition anchor, int rotation) {
        TileType type = tile.getTileType();
        List<HexPosition> piece = shapeCells(type.footprint(), anchor, rotation, false);
        for (int i = 0; i < piece.size(); i++) {
            cells.put(piece.get(i), new Cell(tile, sides(rotation, type.blueSides()[i])));
        }
    }

}
