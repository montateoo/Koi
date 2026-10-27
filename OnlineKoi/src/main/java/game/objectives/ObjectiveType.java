package game.objectives;

import java.util.ArrayList;
import java.util.List;

import game.Player;
import game.Tile;
import game.TileType;
import game.cards.CardType;

public enum ObjectiveType {

    BRANCHES(0, 10, 15, 20) {               // punti: ramo piccolo, medio, grande
        @Override
        public boolean isSatisfiedBy(Player player){
            for (TileType size : BRANCH_SIZES) {
                if (branchSatisfied(size, player)) {
                    return true;
                }
            }
            return false;
        }

        // i rami non sono copie uguali: si prende il piu' grande rimasto tra quelli soddisfatti
        @Override
        public Tile pickTile(Player player, List<Tile> tiles) {
            for (TileType size : BRANCH_SIZES) {
                if (!branchSatisfied(size, player)) {
                    continue;
                }
                for (Tile tile : tiles) {
                    if (tile.getTileType() == size) {
                        return tile;
                    }
                }
            }
            return null;
        }
    },
    TURTLE(0, 0, 1, 2, 4, 8, 12, 16) {      // punti con 0..6 acque libere adiacenti
        @Override
        public boolean isSatisfiedBy(Player player){
            return player.countCards(CardType.SCROLL) >=3 && player.countFreeWater() >= 1;
        }
    },
    THREE(0, 8) {
        @Override
        public boolean isSatisfiedBy(Player player){
            return player.countKoi() >=5 && !player.getPlayablePositions(TileType.THREE).isEmpty();
        }
    },
    WATERFALL(0, 8, 16) {                   // punti: normale, sorgente completamente occupata
        @Override
        public boolean isSatisfiedBy(Player player){
            return player.countCards(CardType.WELL) >= 2
                    && !player.getPlayablePositions(TileType.WATERFALL).isEmpty();
        }
    },
    BRIDGE(3, 1) {                          // punti per esagono in linea davanti
        @Override
        public boolean isSatisfiedBy(Player player) {
            return player.countTops(TileType.LANTERN) >= 1 && !player.getPlayablePositions(TileType.BRIDGE).isEmpty();
        }
    },
    STATUE(0, 4) {                          // punti per carpa in linea davanti
        @Override
        public boolean isSatisfiedBy(Player player) {
            return player.countTops(TileType.LANTERN) >= 2 && !player.getPlayablePositions(TileType.STATUE).isEmpty();
        }
    },
    SHISHIODOSHI(0, 3) {                    // punti per carpa adiacente
        @Override
        public boolean isSatisfiedBy(Player player) {
            return player.hasShape(ObjectiveShapes.SHISHI_ODOSHI, false);
        }
    },
    PURPLELILY(0, 2) {                      // bonus a ogni carpa adiacente (una ninfea normale da' 1)
        @Override
        public boolean isSatisfiedBy(Player player) {
            return player.countTops(TileType.LILY) >= 2 && player.countFreeWater() >= 1 && player.hasAdjacentTops(TileType.LILY);
        }
    },
    PATH(2, 0, 3, 6, 12, 18) {              // punti con 0..4+ tessere
        // basta un posto dove mettere la tessera; si reclama piazzando una carpa (vedi GameRoom.offerPath)
        @Override
        public boolean isSatisfiedBy(Player player) {
            return !player.getPlayablePositions(TileType.PATH).isEmpty();
        }
    },
    OLDCARP(5, 8) {                         // valore base, poi come le carpe normali
        @Override
        public boolean isSatisfiedBy(Player player) {
            return player.hasShape(ObjectiveShapes.OLD_CARP, true);
        }
    };

    private final int cost;
    private final int maxTurtles = 2;
    private final int maxThrees = 2;
    private final int maxWaterfalls = 2;
    private final int maxBridges = 2;
    private final int maxStatues = 2;
    private final int maxShishiOdoshi = 2;
    private final int maxPurpleLilys = 2;
    private final int maxPaths = 7;
    private final int maxOldCarps = 2;

    private final int[] points;   // i valori stampati sulla carta (regolamento pag. 13): li usa ScoreCalculator

    ObjectiveType(int cost, int... points) {
        this.cost = cost;
        this.points = points;
    }

    public int cost() {
        return cost;
    }

    // valore alla posizione i della tabella dei punti; oltre la fine vale l'ultimo
    // (percorso con 5 tessere = come 4+, cioe' 18)
    public int points(int i) {
        return points[Math.min(i, points.length - 1)];
    }

    // per gli obiettivi con un solo valore (salice, statua, shishi odoshi...)
    public int points() {
        return points(0);
    }

    // nome da mostrare ai giocatori
    public String displayName() {
        switch (this) {
            case BRANCHES:     return "Rami";
            case TURTLE:       return "Tartaruga";
            case THREE:        return "Salice piangente";
            case WATERFALL:    return "Cascata";
            case BRIDGE:       return "Ponticello";
            case STATUE:       return "Statua";
            case SHISHIODOSHI: return "Shishi odoshi";
            case PURPLELILY:   return "Ninfea viola";
            case PATH:         return "Percorso";
            default:           return "Carpa centenaria";
        }
    }

    // requisito in breve, come sulla carta obiettivo (il costo in monete si mostra a parte)
    public String requirement() {
        switch (this) {
            case BRANCHES:     return "Acqua libera a forma di ramo + 2/3/4 fogliami (piccolo/medio/grande)";
            case TURTLE:       return "3 carte Pergamena + 1 acqua libera";
            case THREE:        return "5 carpe nel tuo stagno";
            case WATERFALL:    return "2 carte Pozzo; va piazzata davanti alla Sorgente";
            case BRIDGE:       return "1 lanterna; va piazzato accanto a una sponda con la lanterna";
            case STATUE:       return "2 lanterne";
            case SHISHIODOSHI: return "3 acque libere a triangolo (non della Sorgente)";
            case PURPLELILY:   return "2 ninfee adiacenti + 1 acqua libera";
            case PATH:         return "Si reclama piazzando una carpa; ogni tessera allunga il percorso";
            default:           return "4 acque libere a forma di carpa centenaria";
        }
    }

    public abstract boolean isSatisfiedBy(Player player);

    // quale tessera prende il giocatore, null se nessuna: di solito la prima, i rami la scelgono per taglia
    public Tile pickTile(Player player, List<Tile> tiles) {
        return !tiles.isEmpty() && isSatisfiedBy(player) ? tiles.get(0) : null;
    }

    // dal ramo che vale di piu' al piu' piccolo
    private static final TileType[] BRANCH_SIZES = {TileType.BIGBRANCH, TileType.MEDIUMBRANCH, TileType.SMALLBRANCH};

    // forma d'acqua libera (sorgente esclusa) + fogliame ovunque nel laghetto
    private static boolean branchSatisfied(TileType branch, Player player) {
        int leaves = player.countBase(TileType.LEAF);
        switch (branch) {
            case BIGBRANCH:    return leaves >= 4 && player.hasShape(ObjectiveShapes.BIG_BRANCH, false);
            case MEDIUMBRANCH: return leaves >= 3 && player.hasShape(ObjectiveShapes.MEDIUM_BRANCH, false);
            case SMALLBRANCH:  return leaves >= 2 && player.hasShape(ObjectiveShapes.SMALL_BRANCH, false);
            default:           return false;
        }
    }

    public Objective build(int playerNum) {
        switch (this) {
            case BRANCHES:     return buildBranches(playerNum);
            case TURTLE:       return buildTurtle(playerNum);
            case THREE:        return buildThree(playerNum);
            case WATERFALL:    return buildWaterfall(playerNum);
            case BRIDGE:       return buildBridge(playerNum);
            case STATUE:       return buildStatue(playerNum);
            case SHISHIODOSHI: return buildShishiOdoshi(playerNum);
            case PURPLELILY:   return buildPurpleLily(playerNum);
            case PATH:         return buildPath(playerNum);
            default:           return buildOldCarp(playerNum);
        }
    }
    
    public Objective buildBranches(int playerNum){
        ArrayList<Tile> associatedTiles = new ArrayList<>();
        associatedTiles.add(new Tile(TileType.SMALLBRANCH));
        associatedTiles.add(new Tile(TileType.MEDIUMBRANCH));
        associatedTiles.add(new Tile(TileType.BIGBRANCH));

        return new Objective(BRANCHES,associatedTiles);        
    }

    private Objective buildTurtle(int playerNum){
        ArrayList<Tile> associatedTiles = new ArrayList<>();
        associatedTiles.add(new Tile(TileType.TURTLE));

        if(playerNum>2){
            for(int i=1; i<maxTurtles; i++){
                associatedTiles.add(new Tile(TileType.TURTLE));               
            }    
        }

        return new Objective(TURTLE,associatedTiles);
    }

    private Objective buildThree(int playerNum){
        ArrayList<Tile> associatedTiles = new ArrayList<>();
        associatedTiles.add(new Tile(TileType.THREE));

        if(playerNum>2){
            for(int i=1; i<maxThrees; i++){
                associatedTiles.add(new Tile(TileType.THREE));
            }    
        }

        return new Objective(THREE,associatedTiles);        
    }

    private Objective buildWaterfall(int playerNum){
        ArrayList<Tile> associatedTiles = new ArrayList<>();
        associatedTiles.add(new Tile(TileType.WATERFALL));

        if(playerNum>2){
            for(int i=1; i<maxWaterfalls; i++){
                associatedTiles.add(new Tile(TileType.WATERFALL));
            }    
        }

        return new Objective(WATERFALL,associatedTiles);        
    }

    private Objective buildBridge(int playerNum){
        ArrayList<Tile> associatedTiles = new ArrayList<>();
        associatedTiles.add(new Tile(TileType.BRIDGE));

        if(playerNum>2){
            for(int i=1; i<maxBridges; i++){
                associatedTiles.add(new Tile(TileType.BRIDGE));
            }    
        }

        return new Objective(BRIDGE,associatedTiles);
    }

    private Objective buildStatue(int playerNum){
        ArrayList<Tile> associatedTiles = new ArrayList<>();
        associatedTiles.add(new Tile(TileType.STATUE));

        if(playerNum>2){
            for(int i=1; i<maxStatues; i++){
                associatedTiles.add(new Tile(TileType.STATUE));
            }    
        }

        return new Objective(STATUE,associatedTiles);
    }

    private Objective buildShishiOdoshi(int playerNum){
        ArrayList<Tile> associatedTiles = new ArrayList<>();
        associatedTiles.add(new Tile(TileType.SHISHIODOSHI));

        if(playerNum>2){
            for(int i=1; i<maxShishiOdoshi; i++){
                associatedTiles.add(new Tile(TileType.SHISHIODOSHI));
            }    
        }

        return new Objective(SHISHIODOSHI,associatedTiles);        
    }

    private Objective buildPurpleLily(int playerNum){
        ArrayList<Tile> associatedTiles = new ArrayList<>();
        associatedTiles.add(new Tile(TileType.PURPLELILY));

        if(playerNum>2){
            for(int i=1; i<maxPurpleLilys; i++){
                associatedTiles.add(new Tile(TileType.PURPLELILY));
            }    
        }

        return new Objective(PURPLELILY,associatedTiles);        
    }

    private Objective buildPath(int playerNum){
        ArrayList<Tile> associatedTiles = new ArrayList<>();

        for(int i=0; i<maxPaths; i++){
            associatedTiles.add(new Tile(TileType.PATH));
        }

        return new Objective(PATH,associatedTiles);        
    }

    private Objective buildOldCarp(int playerNum){
        ArrayList<Tile> associatedTiles = new ArrayList<>();
        associatedTiles.add(new Tile(TileType.OLDCARP));

        if(playerNum>2){
            for(int i=1; i<maxOldCarps; i++){
                associatedTiles.add(new Tile(TileType.OLDCARP));
            }    
        }

        return new Objective(OLDCARP,associatedTiles);     
    }

}
