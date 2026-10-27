package game;

public class KoiCarp extends Tile {
    // punti per taglia a fine partita (regolamento pag. 11): dipendono dal tipo, non dalla singola carpa
    private static final int SMALL_POINTS = 2;
    private static final int MEDIUM_POINTS = 4;
    private static final int BIG_POINTS = 6;

    private int id;

    public KoiCarp(int id, TileType koyType){
        super(koyType);
        this.id = id;
    }

    // static: basta il tipo, quindi funziona con qualunque tessera carpa (anche una semplice Tile)
    public static int pointsFor(TileType size) {
        return switch (size) {
            case SMALLKOI -> SMALL_POINTS;
            case MEDIUMKOI -> MEDIUM_POINTS;
            case BIGKOI -> BIG_POINTS;
            default -> throw new IllegalArgumentException(size + " is not a koi");
        };
    }

    public int getPoints() {
        return pointsFor(getTileType());
    }

}
