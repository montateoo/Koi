package game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import game.cards.*;

public class Inventory{
    private final int startingSpace = 5;
    private int maxSpace;
    private int spaceLeft;
    private ArrayList<Tile> myTiles;
    private ArrayList<HelperCard> myWorkers;
    private ArrayList<Card> myCards;
    private ArrayList<ScrollCard> myScrolls;
    private int coins;
    private int LuckyCards;

    public Inventory(){
        this.maxSpace = startingSpace;
        this.spaceLeft = startingSpace;
        myTiles = new ArrayList<>();
        myWorkers = new ArrayList<>();
        myCards = new ArrayList<>();
        myScrolls = new ArrayList<>();
    }

    public void addTile(ArrayList<Tile> toShip) {
        for(Tile newTile : toShip){
            addTile(newTile);
        }
    }

    public void addTile(Tile newTile) {
        if(spaceLeft - newTile.getSpaceOccupied() >= 0){
            myTiles.add(newTile);
            spaceLeft = spaceLeft - newTile.getSpaceOccupied();
        }else{
                //TODO gestire lo scarto delle tessere
        }
    }

	public void addLuckyCard() {
        LuckyCards++;
	}

	public void addMoney(int i) {
        coins = coins + i;
    }

	public void addHelper(HelperCard chosenCard) {
        myWorkers.add(chosenCard);
    }
    public void addScroll(ScrollCard chosenCard) {
        myScrolls.add(chosenCard);
    }

    public void addCard(Card chosenCard) {
        myCards.add(chosenCard);
    }

	public void incrementStorage(int space) {
        maxSpace = maxSpace + space;
        spaceLeft = spaceLeft + space;
    }

    public int getCoins() {
        return coins;
    }

    public int getLuckyCards() {
        return LuckyCards;
    }

    public int getMaxSpace() {
        return maxSpace;
    }

    public int getSpaceLeft() {
        return spaceLeft;
    }

    public List<Tile> getTiles() {
        return Collections.unmodifiableList(myTiles);
    }

    public List<HelperCard> getHelpers() {
        return Collections.unmodifiableList(myWorkers);
    }

    public List<Card> getCards() {
        return Collections.unmodifiableList(myCards);
    }

    public List<ScrollCard> getScrolls() {
        return Collections.unmodifiableList(myScrolls);
    }

    public void takeMoney(int cost) {
        coins = coins - cost;  
    }

    public Tile retrieveTile(TileType targetTile) {

        int i = 0;
        for(Tile t: myTiles){
            if(t.getTileType()==targetTile){
                Tile tile = myTiles.remove(i);
                spaceLeft = spaceLeft + tile.getSpaceOccupied();
                return tile;
            }
            i++;
        }

        return null; //TODO molto brutto ma per ora funziona
    }

    public HelperCard searchAmongHelpers(Actions requiredAction) {
        for(HelperCard helper: myWorkers){
            if(helper.canDo(requiredAction)){
                return helper;
            }
        }

        return null; //TODO fare di meglio
    }

    public void restAllWorkers() {
        for(HelperCard worker: myWorkers){
            worker.rest();
        }
    }

    public boolean hasTile(TileType targetType) {
        for(Tile t: myTiles){
            if(t.getTileType() == targetType){
                return true;
            }
        }
        return false;
    }
}