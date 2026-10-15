package game;

public enum TileType {
    WATERSOURCE("water source"),
    WATER("water"),
    LEAF("leaf"),
    LILY("lily"),
    BORDER("border"),
    LANTERN("lantern"),
    BIGKOI("big koi"),
    MEDIUMKOI("medium koi"),
    SMALLKOI("small koi");

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
}
