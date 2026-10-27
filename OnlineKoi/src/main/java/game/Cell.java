package game;

public class Cell {
    private final Tile base;
    private Tile top;
    private final boolean[] blueSides; //i sei lati
    private final boolean lanternSpot;
    private int topRotation;

    public Cell (Tile base, boolean[] blueSides){
        this.base = base;
        this.blueSides = blueSides;
        this.lanternSpot = false;
    }

    public Cell (Tile base, boolean[] blueSides, boolean lanternSpot){
        this.base = base;
        this.blueSides = blueSides;
        this.lanternSpot = lanternSpot;
    }

    public boolean isBlue(int side) {
        return blueSides[side];
    }

    public boolean isFreeWater() {
        return top == null && (base.getTileType() == TileType.WATER || base.getTileType() == TileType.WATERSOURCE);
    }

    // fogliame libero, dove va la ninfea; anche i due esagoni d'acqua del salice piangente sono foglie
    public boolean isFreeLeaf() {
        if (top != null) {
            return false;
        }
        return base.getTileType() == TileType.LEAF || (base.getTileType() == TileType.THREE && hasBlueSide());
    }

    // l'albero e l'erba del salice non hanno lati azzurri, l'acqua con le foglie si'
    private boolean hasBlueSide() {
        for (int side = 0; side < 6; side++) {
            if (isBlue(side)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isFreeBorder() {
		return top == null && lanternSpot;
	}

    public Tile getBase() {
        return base;
    }

    public Tile getTop() {
        return top;
    }

    public int getTopRotation() {
        return topRotation;
    }

    public void setTop(Tile top){
        this.top = top;
    }

    public void setTop(Tile top, int rotation) {
        this.top = top;
        this.topRotation = rotation;
    }

}
