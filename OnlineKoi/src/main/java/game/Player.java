package game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import game.cards.*;
import game.listeners.PlayerListener;

public class Player implements Worker{
    private Color color;
    private String playerName;
    private Inventory inventory;
    private Pond myPond;
    private PlayerListener listener;
    private boolean turnEnded;
    private ArrayList<Actions> allowedActions;
    private PlayerState playerState;
    private int usesLeft;
    private final int waterPlacementUses = 2; //special rule for water tiles
    private final int maxActionsPerTurn = 1;
    private Actions currentAction = null;   // l'azione che sta facendo in questo turno
    private final List<Tile> objectiveTiles = new ArrayList<>();

	public Player(String playerName){
        this.playerName = playerName;
        this.inventory = new Inventory();
        this.myPond = new Pond();
        allowedActions = new ArrayList<>(List.of(Actions.values()));
        playerState = PlayerState.IDLE;
        rest();
    }

    public void setListener(PlayerListener listener){
		this.listener = listener;
	}

    public void give(ArrayList<Tile> toShip){
        inventory.addTile(toShip);
    }

    public void give(Tile toShip) {
        inventory.addTile(toShip);
    }

    public void giveMoney(int i){
        inventory.addMoney(i);
    }

    public void giveLuckyCard(){
        listener.onTakeLuckyCard();
        inventory.addLuckyCard();
    }

    public void working(){
        playerState = PlayerState.WORKING;
    }

    public void meditating(){
        playerState = PlayerState.MEDITATING;
    }

    public void setPlayerIdle(){
        playerState = PlayerState.IDLE;    
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

    public String getName() {
        return playerName;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Pond getPond() {
        return myPond;
    }

    public void isYourTurn(){
       turnEnded=false; 
    }

    public void turnEnded(){
        turnEnded=true;
        setPlayerIdle();
        rest();
        inventory.restAllWorkers();
    }

    public void pay(int cost) {
        inventory.takeMoney(cost);
    }

    public PlayerState getState(){
        return playerState;
    }

    public boolean placeWater(HexPosition position) {
        Worker worker = findWorker(Actions.PLACEWATER);
        if (worker == null) {
            return false;
        }
        worker.work(Actions.PLACEWATER);
        placeTile(TileType.WATER, position);
        return true;
    }

    public boolean placeLeafs(HexPosition position) {
        Worker worker = findWorker(Actions.PLACELEAFS);
        if (worker == null) {
            return false;
        }
        worker.work(Actions.PLACELEAFS);
        placeTile(TileType.LEAF, position);
        return true;
    }

    public void upgradeTile(Actions action, HexPosition position, Tile tile) {
        findWorker(action).work(action);
        myPond.upgrade(position, tile);
    }

    public void placeKoi(TileType koi, Actions action, HexPosition anchor, int rotation) {
        Tile tile = inventory.retrieveTile(koi);
        if (tile == null) {
            throw new IllegalStateException("No " + koi + " in the inventory");
        }
        findWorker(action).work(action);
        myPond.placeKoi(tile, anchor, rotation);
    }

    public void moveKoi(Actions action, int koiId, HexPosition anchor, int rotation) {
        findWorker(action).work(action);
        myPond.moveKoi(koiId, anchor, rotation);
    }

    public boolean canMoveKoi(int koiId, HexPosition anchor, int rotation) {
        return myPond.canMoveKoi(koiId, anchor, rotation);
    }

    public boolean isKoiAt(int koiId, HexPosition anchor, int rotation) {
        return myPond.isKoiAt(koiId, anchor, rotation);
    }

    public Set<Integer> koiIds() {
        return myPond.koiIds();
    }

    public Map<HexPosition, List<Integer>> koiMoves(int koiId) {
        return myPond.koiMoves(koiId);
    }

    public boolean placeTile(TileType tileType, Actions action, HexPosition position) {
        Worker worker = findWorker(action);
        if (worker == null) {
            return false;
        }
        worker.work(action);
        placeTile(tileType, position);
        return true;
    }

    public boolean canPlace(TileType tileType, HexPosition position, int angle) {
        if (tileType == TileType.BORDER) {
            return canPlaceBorder(position, angle);
        }
        if (tileType.isKoi()) {
            return canPlaceKoi(tileType, position, angle);
        }
        return false;
    }


    public boolean canPlaceBorder(HexPosition center, int rotation) {
        return myPond.canPlaceBorder(center, rotation);
    }

    public boolean canPlaceKoi(TileType koi, HexPosition anchor, int rotation) {
        return myPond.canPlaceKoi(koi, anchor, rotation);
    }

    public List<Integer> validRotations(TileType tileType, HexPosition anchor) {
        return myPond.validRotations(tileType, anchor);
    }

    public List<HexPosition> getPlayablePositions(TileType tileType) {
        return myPond.getPlayablePositions(tileType);
    }

	public void placeTile(TileType tileType, HexPosition hexPosition) {
        Tile tile = inventory.retrieveTile(tileType);
        if (tile == null) {
            throw new IllegalStateException("No " + tileType + " tile in the inventory");
        }

        myPond.place(tile, hexPosition);
    }

    public void placeBorder(Actions action, HexPosition center, int rotation) {
        Tile tile = inventory.retrieveTile(TileType.BORDER);
        if (tile == null) {
            throw new IllegalStateException("No border tile in the inventory");
        }
        findWorker(action).work(action);
        myPond.placeBorder(tile, center, rotation);
    }

    @Override
    public boolean canDo(Actions action) {
        return usesLeft > 0 && allowedActions.contains(action)
            && (currentAction == null || currentAction == action);
    }

    @Override
    public boolean isBusy() {
        return currentAction != null && usesLeft > 0;
    }

    @Override
    public void work(Actions action) {
        if (currentAction == null) {
            currentAction = action;
            usesLeft = (action == Actions.PLACEWATER) ? waterPlacementUses : maxActionsPerTurn;
        }
        usesLeft--;
    }

    @Override
    public void rest() {
        usesLeft = maxActionsPerTurn;
        currentAction = null;
    }

    // aiutanti prima, il giocatore per ultimo; tra quelli adatti preferisce chi e' gia' al lavoro
    private Worker findWorker(Actions action) {
        List<Worker> workers = new ArrayList<>(inventory.getHelpers());
        workers.add(this);

        Worker fresh = null;
        for (Worker worker : workers) {
            if (!worker.canDo(action)) {
                continue;
            }
            if (worker.isBusy()) {
                return worker;
            }
            if (fresh == null) {
                fresh = worker;
            }
        }
        return fresh;
    }

    public boolean hasWorkerFor(Actions action) {
        return findWorker(action) != null;
    }

    public int countCards(CardType type) {
        return inventory.countCards(type);
    }

    public int countKoi() {
        return myPond.countKoi();
    }

    public int countTops(TileType type) {
        return myPond.countTops(type);
    }

    public int countFreeWater() {
        return myPond.countFreeWater();
    }

    public boolean hasAdjacentTops(TileType type) {
        return myPond.hasAdjacentTops(type);
    }

    public void addObjective(Tile tile) {
        objectiveTiles.add(tile);
    }

    public List<Tile> getObjectiveTiles() {
        return Collections.unmodifiableList(objectiveTiles);
    }

    public boolean hasShape(int[][] shape, boolean sourceAllowed) {
        return !myPond.findShape(shape, sourceAllowed).isEmpty();
    }

    public int countBase(TileType type) {
        return myPond.countBase(type);
    }

    public boolean hasKoi(int koiId) {
        return !(myPond.findKoi(koiId).isEmpty());
    }

    public void payLuckyCard() {
        inventory.payLuckyCard();
    }

    public List<ScrollCard> getScrolls() {
        return inventory.getScrolls();
    }

}