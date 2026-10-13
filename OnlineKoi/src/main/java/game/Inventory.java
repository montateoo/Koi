package game;

import java.util.ArrayList;

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
            if(spaceLeft - newTile.getSpaceOccupied() >= 0){
                myTiles.add(newTile);
                spaceLeft = spaceLeft - newTile.getSpaceOccupied();
            }else{
                //TODO gestire lo scarto delle tessere
            }
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
}