package game;

import java.util.ArrayList;

import game.cards.*;
import game.listeners.PlayerListener;

public class Player{
    private Color color;
    private Inventory inventory;
    private Pond myPond;
    private PlayerListener listener;

	public Player(){
        this.inventory = new Inventory();
        this.myPond = new Pond();
    }

    public void setListener(PlayerListener listener){
		this.listener = listener;
	}

    public void give(ArrayList<Tile> toShip){
        inventory.addTile(toShip);
    }

    public void giveMoney(int i){
        inventory.addMoney(i);
    }

    public void giveLuckyCard(){
        inventory.addLuckyCard();
    }

    public void giveCard(Card chosenCard){
        if(chosenCard instanceof HelperCard helper){
            inventory.addHelper(helper);
        } else if(chosenCard instanceof ScrollCard scroll){
            inventory.addScroll(scroll);
        } else {
            inventory.addCard(chosenCard);
        }
    }


    public void drawCard(int position){
        listener.onCardDrawn(position);
    }

	public void incrementStorage(int space) {
        inventory.incrementStorage(space);
    }

}