package game;

import java.util.ArrayList;

public class Warehouse {
    private final int maxWater = 80;
    private final int maxLeaf = 30;
    private final int maxLily = 30;
    private final int maxBorder = 15;
    private final int maxLanterns = 15;
    private final int maxSmallBranch = 1;
    private final int maxMediumBranch = 1;
    private final int maxBigBranch = 1;
    private final int maxTurtle = 2;
    private final int maxThree = 2;
    private final int maxWaterfall = 2;
    private final int maxBridge = 2;
    private final int maxStatue = 2;
    private final int maxShishiOdoshi = 2;
    private final int maxPurpleLily = 2;
    private final int maxPath = 7;
    private final int maxOldKoi = 2;
    private final int maxCoins = 30;
    private final int maxLuckyCards = 12;
    private final int maxWarehouseExpansion = 6;

    private int countWater;
    private int countLeaf;
    private int countLily;
    private int countBorder;
    private int countLanterns;
    private int countSmallBranch;
    private int countMediumBranch;
    private int countBigBranch;
    private int countTurtle;
    private int countThree;
    private int countWaterfall;
    private int countBridge;
    private int countStatue;
    private int countShishiOdoshi;
    private int countPurpleLily;
    private int countPath;
    private int countOldKoi;
    private int countCoins;
    private int countLuckyCards;
    private int countWarehouseExpansion;

    public Warehouse(){
        this.countWater = maxWater;
        this.countLeaf = maxLeaf;
        this.countLily = maxLily;
        this.countBorder = maxBorder;
        this.countLanterns = maxLanterns;
        this.countSmallBranch = maxSmallBranch;
        this.countMediumBranch = maxMediumBranch;
        this.countBigBranch = maxBigBranch;
        this.countTurtle = maxTurtle;
        this.countThree = maxThree;
        this.countWaterfall = maxWaterfall;
        this.countBridge = maxBridge;
        this.countStatue = maxStatue;
        this.countShishiOdoshi = maxShishiOdoshi;
        this.countPurpleLily = maxPurpleLily;
        this.countPath = maxPath;
        this.countOldKoi = maxOldKoi;
        this.countCoins = maxCoins;
        this.countLuckyCards = maxLuckyCards;
        this.countWarehouseExpansion = maxWarehouseExpansion;
        
    }

    public ArrayList<Tile> takeWater(int i) {
       
        ArrayList<Tile> toShip = new ArrayList<>();

        for(int j=0; j<i; j++)
        {
            if(countWater>0){
              countWater--;
              toShip.add(new Tile(TileType.WATER));
            }          
        }
        return toShip;
    }

    public ArrayList<Tile> takeBorder(int i) {
        ArrayList<Tile> toShip = new ArrayList<>();

        for(int j=0; j<i; j++)
        {
            if(countBorder>0){
              countBorder--;
              toShip.add(new Tile(TileType.BORDER));
            }          
        }
        return toShip;
    }

    public ArrayList<Tile> takeLeaf(int i) {
        ArrayList<Tile> toShip = new ArrayList<>();

        for(int j=0; j<i; j++)
        {
            if(countLeaf>0){
              countLeaf--;
              toShip.add(new Tile(TileType.LEAF));
            }          
        }
        return toShip;
    }
    
    public ArrayList<Tile> takeLily(int i) {
        ArrayList<Tile> toShip = new ArrayList<>();

        for(int j=0; j<i; j++)
        {
            if(countLily>0){
              countLily--;
              toShip.add(new Tile(TileType.LILY));
            }          
        }
        return toShip;
    }

    public ArrayList<Tile> takeLantern(int i) {
        ArrayList<Tile> toShip = new ArrayList<>();

        for(int j=0; j<i; j++)
        {
            if(countLanterns>0){
              countLanterns--;
              toShip.add(new Tile(TileType.LANTERN));
            }          
        }
        return toShip;
    } 
    
    public void takeLuckyCard() {
        countLuckyCards--;
    }

    public void giveBackLuckyCard() {
        countLuckyCards++;
    }  

    public boolean has(TileType tileType) {
        switch (tileType) {
            case WATER:
                return countWater > 0;
            case LEAF:
                return countLeaf > 0;
            case LILY:
                return countLily > 0;
            case BORDER:
                return countBorder > 0;
            case LANTERN:
                return countLanterns > 0;
            default:
                return false;   // sorgente e carpe non stanno in questa riserva
        }
    }
}
