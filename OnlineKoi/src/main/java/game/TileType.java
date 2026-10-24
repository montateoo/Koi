package game;

public enum TileType {
    WATERSOURCE("water source"){@Override public boolean isTopTile(){return false;}},
    WATER("water"){@Override public boolean isTopTile(){return false;}},
    LEAF("leaf"){@Override public boolean isTopTile(){return false;}},
    LILY("lily"){@Override public boolean isTopTile(){return true;}},
    BORDER("border"){@Override public boolean isTopTile(){return false;}},
    LANTERN("lantern"){@Override public boolean isTopTile(){return true;}},
    BIGKOI("big koi"){@Override public boolean isTopTile(){return true;}},
    MEDIUMKOI("medium koi"){@Override public boolean isTopTile(){return true;}},
    SMALLKOI("small koi"){@Override public boolean isTopTile(){return true;}},
    SMALLBRANCH("small branch"){@Override public boolean isTopTile(){return true;}},
    MEDIUMBRANCH("medium branch"){@Override public boolean isTopTile(){return true;}},
    BIGBRANCH("big branch"){@Override public boolean isTopTile(){return true;}},
    TURTLE("turtle"){@Override public boolean isTopTile(){return true;}},
    THREE("three"){@Override public boolean isTopTile(){return false;}},
    WATERFALL("waterfall"){@Override public boolean isTopTile(){return false;}},
    BRIDGE("bridge"){@Override public boolean isTopTile(){return false;}},
    STATUE("statue"){@Override public boolean isTopTile(){return false;}},
    SHISHIODOSHI("shishiodoshi"){@Override public boolean isTopTile(){return true;}},
    PURPLELILY("purplelily"){@Override public boolean isTopTile(){return true;}},
    PATH("path"){@Override public boolean isTopTile(){return true;}},
    OLDCARP("oldcarp"){@Override public boolean isTopTile(){return true;}};

    private final String displayName;   // nome leggibile da mostrare nei messaggi ai giocatori

    TileType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isKoi() {
        return this == SMALLKOI || this == MEDIUMKOI || this == BIGKOI;
    }

    // tessere che si ottengono reclamando un obiettivo
    public boolean isObjective() {
        return ordinal() >= SMALLBRANCH.ordinal();
    }

    public abstract boolean isTopTile();

    // esagoni della tessera a rotazione 0, come percorsi di direzioni dall'ancoraggio (vedi Pond.shapeCells)
    public int[][] footprint() {
        switch (this) {
            case THREE: return new int[][] {{}, {1}, {0}, {1, 0}};   // albero, erba a NE, acqua a SE e a E
            default:    return new int[][] {{}};
        }
    }

    // per ogni esagono di footprint(), i lati azzurri a rotazione 0
    public int[][] blueSides() {
        switch (this) {
            case THREE: return new int[][] {{}, {}, {0, 4, 5}, {0, 1, 2, 5}};   // l'erba non ne ha, i sassi separano l'acqua dall'erba
            default:    return new int[][] {{0, 1, 2, 3, 4, 5}};
        }
    }


}