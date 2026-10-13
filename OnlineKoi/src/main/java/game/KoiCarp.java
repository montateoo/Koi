package game;

public class KoiCarp extends Tile {
    private int id;
    private final int bigKoyPoints = 6;
    private final int mediumKoyPoints = 4;
    private final int smallKoyPoints = 2;

    public KoiCarp(int id, TileType koyType){
        super(koyType);
        this.id = id;
    }

}
