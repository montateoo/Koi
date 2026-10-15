package game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public class Tile {

    private static final AtomicInteger count = new AtomicInteger(0); 
    private Integer tileID; //per tessere composte da piuò parti aggrego unità con lo stesso id
    private TileType tileType;
    private int spaceOccupied;

	public Tile(TileType tileType) {
		this.tileType = tileType;
        this.tileID = count.incrementAndGet();

        switch (tileType){
            case WATER:
                this.spaceOccupied = 1;
                break;
            case LEAF:
                this.spaceOccupied = 1;
                break;
            case LILY:
                this.spaceOccupied = 0;
                break;
            case BORDER:
                this.spaceOccupied = 3;
                break;
            case SMALLKOI:
                this.spaceOccupied = 1;
                break;
            case MEDIUMKOI:
                this.spaceOccupied = 2;
                break;
            case BIGKOI:
                this.spaceOccupied = 3;
                break;
            case WATERSOURCE:
                this.spaceOccupied = 3;
                break;
            default:
                break;           
        }
	}

    public int getTileID() {
        return tileID;
    }

    public int getSpaceOccupied() {
        return spaceOccupied;
    }

    public TileType getTileType() {
        return tileType;
    }
    
}
