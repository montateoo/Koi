package game;

import java.util.ArrayList;
import java.util.Random;

import game.cards.*;
import game.listeners.*;

public class GameRoom implements CardEffects, BoardListener, WarehouseListener, PlayerListener {
    private ArrayList<Player> players;
    private Board board;
    private KoiMarket koiMarket;
    private Warehouse warehouse;
    private Player activePlayer; //giocatore a cui spetta giocare
    private int activePlayerIndex;
    private final int maxAllowedPlayers = 4;
    private int playerNum;

    public GameRoom(){
        this.board = new Board();
        board.setListener(this);
        this.koiMarket = new KoiMarket();
        this.warehouse = new Warehouse();
        this.players = new ArrayList<>();
    }

    public Board getBoard(){
        return board;
    }

    public KoiMarket getMarket(){
        return koiMarket;
    }

    @Override
	public void applyBoardEffect(int position) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'applyBoardEffect'");
	}

    @Override
    public void onCardDrawn(int position) {
        Card card = board.drawCard(position);
        activePlayer.giveCard(card);
        card.activate(this);
    }       

    @Override
	public void onGiveComand(int position) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'onGiveComand'");
	}

    @Override
	public void goToMarketWithDiscount(int amount) {
		//TODO gestire andare al Mercato
	}

    @Override
	public void placeRainTiles(ArrayList<Tile> rainTiles) {
        //TODO gestire piazzamento immediato tessere 
    }

	@Override
	public void addStorage(int space) {
        activePlayer.incrementStorage(space);
    }

    @Override
	public void giveWellMoney(int money) {
        activePlayer.giveMoney(money);
	}

	@Override
	public void giveWellWater(int water) {
        ArrayList<Tile> toShip;
        toShip = warehouse.takeWater(2);
        activePlayer.give(toShip);        
    }

	@Override
	public void giveWellChoice(int money, int water) {
		//TODO gestire la scelta
	}


    public void addPlayer(Player newPlayer){
        if(players.size()<maxAllowedPlayers){
            newPlayer.setListener(this);
            players.add(newPlayer);
        }
    }

    private void pickStartingPlayer(){
        activePlayer = players.get(new Random().nextInt(playerNum));
        activePlayerIndex = players.indexOf(activePlayer);
	}

    private void goToNextPlayer(){
        if((activePlayerIndex + 1) == playerNum){
            activePlayerIndex = 0;
        } else {
            activePlayerIndex++;
        }
        activePlayer = players.get(activePlayerIndex);
    }

	private void giveStartingTiles() {
        ArrayList<Tile> toShip;
		toShip = warehouse.takeWater(2);
        players.get(activePlayerIndex).give(toShip);

        if(playerNum>1){
            goToNextPlayer();
            toShip = warehouse.takeWater(2);
            toShip.add(koiMarket.takeSmallKoi());
            players.get(activePlayerIndex).give(toShip);
        }
        if(playerNum>2){
            goToNextPlayer();
            toShip = warehouse.takeWater(2);
            toShip.add(koiMarket.takeMediumKoi());
            players.get(activePlayerIndex).give(toShip);
        }
        if(playerNum>3){
            goToNextPlayer();
            toShip = warehouse.takeWater(2);
            toShip.add(koiMarket.takeBigKoi());
            players.get(activePlayerIndex).give(toShip);
        }

        goToNextPlayer();
	}

    private void giveStartingKit() {
        for(Player p: players)
        {
            p.giveMoney(2);
            p.giveLuckyCard();
        }
	}

    public void prepareGame(){

        playerNum = players.size();

        koiMarket.initializeMarket(playerNum);
        board.initializeBoard(playerNum);
        
        pickStartingPlayer();

        giveStartingTiles();
        giveStartingKit();

        //pickObjectives();

    }    
}
