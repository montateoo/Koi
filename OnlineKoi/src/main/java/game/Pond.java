package game;

import java.util.ArrayList;

public class Pond {
    ArrayList<Tile> myPond;

    public Pond(){
        myPond = new ArrayList<>();
        myPond.add(new Tile(TileType.WATERSOURCE));
    }

}
