package game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.ArrayDeque;
import java.util.Deque;

import game.cards.*;
import game.listeners.*;
import game.objectives.Objective;
import game.objectives.ObjectiveController;
import game.objectives.ObjectiveType;

public class GameRoom implements CardEffects, BoardListener, WarehouseListener, PlayerListener {

    private ArrayList<Player> players;
    private Board board;
    private KoiMarket koiMarket;
    private Warehouse warehouse;
    private ObjectiveController objectiveController;
    private Player activePlayer; //giocatore a cui spetta giocare
    private int activePlayerIndex;
    private final int maxAllowedPlayers = 4;
    private final Deque<PendingChoice> pendingChoices = new ArrayDeque<>();
    private int playerNum;

    public GameRoom(){
        this.board = new Board();
        board.setListener(this);
        this.koiMarket = new KoiMarket();
        this.warehouse = new Warehouse();
        this.objectiveController = new ObjectiveController();
        this.players = new ArrayList<>();
        playerNum = 0;
    }

    public Board getBoard(){
        return board;
    }

    public KoiMarket getMarket(){
        return koiMarket;
    }

    public List<Player> getPlayers(){
        return Collections.unmodifiableList(players);
    }

    public List<Objective> getObjectives() {
        return objectiveController.getObjectives();
    }

    public Player getActivePlayer(){
        return activePlayer;
    }

    @Override
    public void playerHasToPay(int cost) {
        activePlayer.pay(cost);
    }

    @Override
    public void playerGiveBonusMoneyWater(int secondBonusMoney, int secondBonusWater) {
        activePlayer.giveMoney(secondBonusMoney);

        ArrayList<Tile> toShip = warehouse.takeWater(secondBonusWater);
        activePlayer.give(toShip);
    }

    @Override
    public void playerAskBonusOrMarket(int money, int water) {
        pendingChoices.add(new PendingChoice(activePlayer, "Take the bonus or go to the market?",
                List.of(money + " coins and " + water + " water", "Go to the koi market"),
                option -> {
                    if (option == 0) {
                        playerGiveBonusMoneyWater(money, water);
                    } else {
                        goToMarket();
                    }
                }));
    }

    @Override
    public void playerAskBorderOrLeaf(int fifthBonusBorder, int fifthBonusLeaf) {
        pendingChoices.add(new PendingChoice(activePlayer, "Take a border tile or a leaf tile?",
                List.of(fifthBonusBorder + " border tile", fifthBonusLeaf + " leaf tile"),
                option -> {
                    if (option == 0) {
                        activePlayer.give(warehouse.takeBorder(fifthBonusBorder));
                    } else {
                        activePlayer.give(warehouse.takeLeaf(fifthBonusLeaf));
                    }
                }));
    }

    @Override
    public void onCardDrawn(int position) {
        Card card = board.drawCard(position);
        activePlayer.giveCard(card);
        card.activate(this);
    }  

    @Override
    public void onTakeLuckyCard() {
        warehouse.takeLuckyCard();
    }
    
    @Override
	public void onGiveComand(int position) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'onGiveComand'");
	}

    @Override
	public void goToMarketWithDiscount(int amount) {
		pendingChoices.add(new PendingChoice(activePlayer, "which koi do you wish to buy?",
                List.of("Small koi", "Medium koi", "Big koi"),
                option -> {
                    if (option == 0) {
                        activePlayer.pay(Math.max(0, koiMarket.getSmallKoiCost() - amount));
                        activePlayer.give(koiMarket.takeSmallKoi());
                    } else if (option == 1) {
                        activePlayer.pay(Math.max(0, koiMarket.getMediumKoiCost() - amount));
                        activePlayer.give(koiMarket.takeMediumKoi());
                    } else if (option == 2) {
                        activePlayer.pay(Math.max(0, koiMarket.getBigKoiCost() - amount));
                        activePlayer.give(koiMarket.takeBigKoi());
                    }
                }));
	}

    private void goToMarket(){
        goToMarketWithDiscount(0);
    }

    @Override
    public void placeRainTiles(ArrayList<Tile> rainTiles) {
        List<TileType> types = new ArrayList<>();
        for (Tile tile : rainTiles) {
            types.add(tile.getTileType());
        }
        askRainPlacement(activePlayer, types);
    }

    // una domanda per tessera: la successiva si chiede solo dopo aver piazzato questa, perche' le posizioni cambiano
    private void askRainPlacement(Player player, List<TileType> remaining) {
        if (remaining.isEmpty()) {
            return;
        }
        TileType type = remaining.get(0);
        List<TileType> rest = new ArrayList<>(remaining.subList(1, remaining.size()));
        List<HexPosition> positions = player.getPlayablePositions(type);

        if (positions.isEmpty() || !warehouse.has(type)) {
            askRainPlacement(player, rest);   // nessun posto o riserva vuota: questa tessera si salta
            return;
        }

        List<String> labels = new ArrayList<>();
        for (HexPosition position : positions) {
            labels.add(position.toString());
        }
        pendingChoices.add(new PendingChoice(player,
                "Rain: where do you place the " + type.displayName() + "?", labels,
                option -> {
                    player.getPond().place(takeFromReserve(type), positions.get(option));
                    askRainPlacement(player, rest);
                }));
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
        toShip = warehouse.takeWater(water);
        activePlayer.give(toShip);        
    }

	@Override
	public void giveWellChoice(int money, int water) {
        pendingChoices.add(new PendingChoice(activePlayer, "which option do you prefer?",
                List.of(money + " coins", water + " water tile"),
                option -> {
                    if (option == 0) {
                        activePlayer.giveMoney(money);
                    } else {
                        activePlayer.give(warehouse.takeWater(water));
                    }
                }));
    }

    public PendingChoice getPendingChoice() {
        return pendingChoices.peek();
    }

    public void answerChoice(int option) {
        pendingChoices.poll().onAnswer().accept(option);
    }


    public void addPlayer(Player newPlayer){
        if(players.size()<maxAllowedPlayers){
            newPlayer.setListener(this);
            players.add(newPlayer);
            playerNum++;
        }
    }

    private void pickStartingPlayer(){
        activePlayer = players.get(new Random().nextInt(playerNum));
        activePlayerIndex = players.indexOf(activePlayer);
	}

    public void goToNextPlayer(){
        if((activePlayerIndex + 1) == playerNum){
            activePlayerIndex = 0;
        } else {
            activePlayerIndex++;
        }
        activePlayer = players.get(activePlayerIndex);
        activePlayer.isYourTurn();
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

    public boolean isFull() {
        if(playerNum==maxAllowedPlayers){
            return true;
        } else {
            return false;
        }
    }

    public void prepareGame(){

        koiMarket.initializeMarket(playerNum);
        board.initializeBoard(playerNum);
        
        pickStartingPlayer();

        giveStartingTiles();
        giveStartingKit();

        objectiveController.initializeObjectives(playerNum);

        startTurnRotation();

    }

    private void startTurnRotation(){
        activePlayer.isYourTurn();
    }

    public boolean reserveHas(TileType tileType) {
        return warehouse.has(tileType);
    }

    public Tile takeFromReserve(TileType tileType) {
        switch (tileType) {
            case WATER:
                return warehouse.takeWater(1).getFirst();
            case LEAF:
                return warehouse.takeLeaf(1).getFirst();
            case LILY:
                return warehouse.takeLily(1).getFirst();
            case BORDER:
                return warehouse.takeBorder(1).getFirst();
            case LANTERN:
                return warehouse.takeLantern(1).getFirst();
            default:
                return null;
        }
    }

    // Percorso: si offre ogni volta che il giocatore piazza una carpa, anche piu' volte nella stessa partita
    public void offerPath(Player player) {
        Objective path = objectiveController.find(ObjectiveType.PATH);
        if (path == null || !path.canBeClaimedBy(player)) {
            return;
        }
        pendingChoices.add(new PendingChoice(player,
                "You placed a koi: do you want a path tile for " + ObjectiveType.PATH.cost() + " coins?",
                List.of("Take a path tile", "No thanks"),
                option -> {
                    if (option == 0) {
                        askPathPlacement(player, path.claim(player));
                    }
                }));
    }

    // gli obiettivi vanno piazzati subito: domanda con coordinate, cosi' nella GUI si clicca lo stagno
    private void askPathPlacement(Player player, Tile tile) {
        List<HexPosition> positions = player.getPlayablePositions(TileType.PATH);
        List<String> labels = new ArrayList<>();
        for (HexPosition position : positions) {
            labels.add(position.toString());
        }
        pendingChoices.add(new PendingChoice(player, "Where do you place the path tile?", labels,
                option -> player.getPond().placePath(tile, positions.get(option))));
    }

    // obiettivi con una forma (rami, shishi odoshi, carpa centenaria, tartaruga, ninfea viola):
    // prima dove (coordinate, si clicca lo stagno), poi come orientarla se c'e' piu' di un modo; infine "then"
    private void askObjectivePlacement(Player player, Tile tile, Runnable then) {
        TileType type = tile.getTileType();
        List<HexPosition> positions = player.getPlayablePositions(type);
        if (positions.isEmpty()) {
            player.addObjective(tile);   // non dovrebbe succedere: il requisito garantisce un posto
            then.run();
            return;
        }
        List<String> labels = new ArrayList<>();
        for (HexPosition position : positions) {
            labels.add(position.toString());
        }
        pendingChoices.add(new PendingChoice(player, "Where do you place the " + type.displayName() + "?", labels,
                option -> askObjectiveRotation(player, tile, positions.get(option), then)));
    }

    private void askObjectiveRotation(Player player, Tile tile, HexPosition anchor, Runnable then) {
        List<Integer> rotations = player.validRotations(tile.getTileType(), anchor);
        if (rotations.size() == 1) {
            player.getPond().placeObjective(tile, anchor, rotations.get(0));
            then.run();
            return;
        }
        List<String> labels = new ArrayList<>();
        for (int rotation : rotations) {
            labels.add("Rotation " + (rotation % 6) + (rotation >= 6 ? " (mirrored)" : ""));
        }
        pendingChoices.add(new PendingChoice(player, "How do you turn the " + tile.getTileType().displayName() + "?", labels,
                option -> {
                    player.getPond().placeObjective(tile, anchor, rotations.get(option));
                    then.run();
                }));
    }

    // tessere che askObjectivePlacement sa gia' piazzare; le altre (ponticello, statua...) per ora restano da parte
    private static boolean placedByShape(TileType type) {
        switch (type) {
            case SMALLBRANCH:
            case MEDIUMBRANCH:
            case BIGBRANCH:
            case SHISHIODOSHI:
            case OLDCARP:
            case TURTLE:
            case PURPLELILY:
            case STATUE:
            case BRIDGE:
            case WATERFALL:
            case THREE:
                return true;
            default:
                return false;
        }
    }

    // true se ha messo in coda la domanda: il turno finisce dopo la risposta
    public boolean checkObjectivesforPlayer(Player player) {
        List<Objective> claimable = objectiveController.checkCompletedObjectives(player);
        if (claimable.isEmpty()) {
            return false;
        }

        List<String> options = new ArrayList<>();
        for (Objective objective : claimable) {
            options.add(objective.describeFor(player));
        }
        options.add("No thanks");

        pendingChoices.add(new PendingChoice(player, "You can claim an objective: do you want it?", options,
                option -> {
                    if (option == claimable.size()) {
                        return;                                   // "No thanks"
                    }
                    Tile tile = claimable.get(option).claim(player);
                    // magari ne puo' prendere un altro: si ricontrolla solo dopo il piazzamento, che toglie acqua libera
                    if (placedByShape(tile.getTileType())) {
                        askObjectivePlacement(player, tile, () -> checkObjectivesforPlayer(player));
                    } else {
                        player.addObjective(tile);
                        checkObjectivesforPlayer(player);
                    }
                }));
        return true;
    }

}
