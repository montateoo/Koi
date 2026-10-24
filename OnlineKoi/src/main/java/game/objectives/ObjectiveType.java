package game.objectives;

import java.util.ArrayList;
import java.util.List;

import game.Player;
import game.Tile;
import game.TileType;
import game.cards.CardType;

public enum ObjectiveType {

    BRANCHES(0) {
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
    TURTLE(0) {
        @Override
        public boolean isSatisfiedBy(Player player){
            return player.countCards(CardType.SCROLL) >=3 && player.countFreeWater() >= 1;
        }
    },
    THREE(0) {
        @Override
        public boolean isSatisfiedBy(Player player){
            return player.countKoi() >=5 && !player.getPlayablePositions(TileType.THREE).isEmpty();
        }
    },
    WATERFALL(0) {
        @Override
        public boolean isSatisfiedBy(Player player){
            return player.countCards(CardType.WELL) >= 2
                    && !player.getPlayablePositions(TileType.WATERFALL).isEmpty();
        }
    },
    BRIDGE(3) {
        @Override
        public boolean isSatisfiedBy(Player player) {
            return player.countTops(TileType.LANTERN) >= 1 && !player.getPlayablePositions(TileType.BRIDGE).isEmpty();
        }
    },
    STATUE(0) {
        @Override
        public boolean isSatisfiedBy(Player player) {
            return player.countTops(TileType.LANTERN) >= 2 && !player.getPlayablePositions(TileType.STATUE).isEmpty();
        }
    },
    SHISHIODOSHI(0) {
        @Override
        public boolean isSatisfiedBy(Player player) {
            return player.hasShape(ObjectiveShapes.SHISHI_ODOSHI, false);
        }
    },
    PURPLELILY(0) {
        @Override
        public boolean isSatisfiedBy(Player player) {
            return player.countTops(TileType.LILY) >= 2 && player.countFreeWater() >= 1 && player.hasAdjacentTops(TileType.LILY);
        }
    },
    PATH(2) {
        // basta un posto dove mettere la tessera; si reclama piazzando una carpa (vedi GameRoom.offerPath)
        @Override
        public boolean isSatisfiedBy(Player player) {
            return !player.getPlayablePositions(TileType.PATH).isEmpty();
        }
    },
    OLDCARP(5) {
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

    ObjectiveType(int cost) {
        this.cost = cost;
    }

    public int cost() {
        return cost;
    }

    // nome da mostrare ai giocatori
    public String displayName() {
        switch (this) {
            case BRANCHES:     return "Branches";
            case TURTLE:       return "Turtle";
            case THREE:        return "Weeping willow";
            case WATERFALL:    return "Waterfall";
            case BRIDGE:       return "Bridge";
            case STATUE:       return "Statue";
            case SHISHIODOSHI: return "Shishi odoshi";
            case PURPLELILY:   return "Purple lily";
            case PATH:         return "Path";
            default:           return "Old carp";
        }
    }

    // requisito in breve, come sulla carta obiettivo (il costo in monete si mostra a parte)
    public String requirement() {
        switch (this) {
            case BRANCHES:     return "Free water in a branch shape + 2/3/4 leaves (small/medium/big)";
            case TURTLE:       return "3 scroll cards + 1 free water";
            case THREE:        return "5 koi in your pond";
            case WATERFALL:    return "2 well cards; placed facing the source";
            case BRIDGE:       return "1 lantern; placed next to a border with a lantern";
            case STATUE:       return "2 lanterns";
            case SHISHIODOSHI: return "3 free water in a triangle (not the source)";
            case PURPLELILY:   return "2 adjacent lilies + 1 free water";
            case PATH:         return "Claim it when you place a koi; extend the path each time";
            default:           return "4 free water in the old carp shape";
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
