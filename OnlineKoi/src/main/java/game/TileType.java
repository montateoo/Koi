package game;

public enum TileType {
    WATERSOURCE("sorgente"){@Override public boolean isTopTile(){return false;}},
    WATER("acqua"){@Override public boolean isTopTile(){return false;}},
    LEAF("fogliame"){@Override public boolean isTopTile(){return false;}},
    LILY("ninfea"){@Override public boolean isTopTile(){return true;}},
    BORDER("sponda"){@Override public boolean isTopTile(){return false;}},
    LANTERN("lanterna"){@Override public boolean isTopTile(){return true;}},
    BIGKOI("carpa grande"){@Override public boolean isTopTile(){return true;}},
    MEDIUMKOI("carpa media"){@Override public boolean isTopTile(){return true;}},
    SMALLKOI("carpa piccola"){@Override public boolean isTopTile(){return true;}},
    SMALLBRANCH("ramo piccolo"){@Override public boolean isTopTile(){return true;}},
    MEDIUMBRANCH("ramo medio"){@Override public boolean isTopTile(){return true;}},
    BIGBRANCH("ramo grande"){@Override public boolean isTopTile(){return true;}},
    TURTLE("tartaruga"){@Override public boolean isTopTile(){return true;}},
    THREE("salice piangente"){@Override public boolean isTopTile(){return false;}},
    WATERFALL("cascata"){@Override public boolean isTopTile(){return false;}},
    BRIDGE("ponticello"){@Override public boolean isTopTile(){return false;}},
    STATUE("statua"){@Override public boolean isTopTile(){return false;}},
    SHISHIODOSHI("shishi odoshi"){@Override public boolean isTopTile(){return true;}},
    PURPLELILY("ninfea viola"){@Override public boolean isTopTile(){return true;}},
    PATH("percorso"){@Override public boolean isTopTile(){return true;}},
    OLDCARP("carpa centenaria"){@Override public boolean isTopTile(){return true;}};

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