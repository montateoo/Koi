package game;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Tile {

    private static final AtomicInteger count = new AtomicInteger(0); 
    private Integer tileID; //per tessere composte da piuò parti aggrego unità con lo stesso id
    private TileType tileType;
    private ArrayList<Integer> corners; //sono esagoni quindi a partire dal lato alto in senso orario 0,1,2,3,4,5
    private int spaceOccupied;

	public Tile(TileType tileType) {
		this.tileType = tileType;
        this.tileID = count.incrementAndGet();

        switch (tileType){
            case TileType.WATER:
                corners = new ArrayList<>(6);
                corners.addAll(null);
                this.spaceOccupied = 1;
                break;
            case TileType.LEAF:
                corners = new ArrayList<>(6);
                corners.addAll(null);
                this.spaceOccupied = 1;

                break;
            case TileType.LILY:
                corners = new ArrayList<>(6);
                corners.addAll(null);
                this.spaceOccupied = 0;
                break;
            case TileType.BORDER:
                corners = new ArrayList<>(18);
                corners.addAll(null);
                corners.add(2,this.tileID);
                corners.add(4,this.tileID);
                corners.add(11,this.tileID);
                corners.add(13,this.tileID);
                this.spaceOccupied = 3;
                break;
            case TileType.SMALLKOI:
                corners = new ArrayList<>(6);
                corners.addAll(null);
                this.spaceOccupied = 1;
                break;
            case TileType.MEDIUMKOI:
                corners = new ArrayList<>(12);
                corners.addAll(null);
                corners.add(3,this.tileID);
                corners.add(6,this.tileID);
                this.spaceOccupied = 2;
                break;
            case TileType.BIGKOI:
                corners = new ArrayList<>(18);
                corners.addAll(null);
                corners.add(3,this.tileID);
                corners.add(4,this.tileID);
                corners.add(6,this.tileID);
                corners.add(11,this.tileID);
                corners.add(12,this.tileID);
                corners.add(13,this.tileID);
                this.spaceOccupied = 3;
                break;
            case TileType.WATERSOURCE:
                corners = new ArrayList<>(30);
                corners.addAll(null);
                corners.add(1,this.tileID);
                corners.add(2,this.tileID);
                corners.add(4,this.tileID);
                corners.add(5,this.tileID);
                corners.add(9,this.tileID);
                corners.add(10,this.tileID);
                corners.add(11,this.tileID);
                corners.add(17,this.tileID);
                corners.add(18,this.tileID);
                corners.add(19,this.tileID);
                corners.add(26,this.tileID);
                corners.add(27,this.tileID);
                this.spaceOccupied = 3;
                break;
            default:
                break;           
        }
	}

    public int getSpaceOccupied() {
        return spaceOccupied;
    }
    
}
