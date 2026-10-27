package game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

import game.cards.*;
import game.listeners.*;
import game.objectives.Objective;
import game.objectives.ObjectiveController;
import game.objectives.ObjectiveType;
import game.scoring.ScoreCalculator;
import game.scoring.ScoreSheet;

public class GameRoom implements CardEffects, BoardListener, WarehouseListener, PlayerListener, MarketListener {

    private ArrayList<Player> players;
    private Board board;
    private KoiMarket koiMarket;
    private Warehouse warehouse;
    private ObjectiveController objectiveController;
    private Player activePlayer; //giocatore a cui spetta giocare
    private int activePlayerIndex;
    private int closingPlayerIndex; //il giocatore che conclude il turno, serve per le fasi finali di gioco
    private final int maxAllowedPlayers = 4;
    private final Deque<PendingChoice> pendingChoices = new ArrayDeque<>();

    // segnalino Fortuna (regolamento pag. 6): 1 fogliame, 2 acqua o 1 sponda, oppure una carpa a 1 moneta in piu'
    private static final int LUCKY_LEAVES = 1;
    private static final int LUCKY_WATER = 2;
    private static final int LUCKY_BORDER = 1;
    private static final int LUCKY_KOI_EXTRA = 1;
    private int playerNum;

    private boolean gameIsEnding;
    private boolean finalWorkingPhase;
    private boolean over;   // il giocatore di chiusura ha finito il turno finale: si contano i punti
    private List<ScoreSheet> finalScores = List.of();   // classifica finale, dal primo all'ultimo

    public GameRoom(){
        this.board = new Board();
        board.setListener(this);
        this.koiMarket = new KoiMarket();
        koiMarket.setListener(this);
        this.warehouse = new Warehouse();
        this.objectiveController = new ObjectiveController();
        this.players = new ArrayList<>();
        playerNum = 0;
        gameIsEnding = false;
        finalWorkingPhase = false;
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
        pendingChoices.add(new PendingChoice(activePlayer, "Prendi il bonus o vai al mercato?",
                List.of(money + " monete e " + water + " acqua", "Vai al mercato delle carpe"),
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
        pendingChoices.add(new PendingChoice(activePlayer, "Prendi una sponda o un fogliame?",
                List.of(fifthBonusBorder + " sponda", fifthBonusLeaf + " fogliame"),
                option -> {
                    if (option == 0) {
                        activePlayer.give(warehouse.takeBorder(fifthBonusBorder));
                    } else {
                        activePlayer.give(warehouse.takeLeaf(fifthBonusLeaf));
                    }
                }));
    }

    // si propongono solo le opzioni possibili adesso (tessere ancora in riserva, carpe disponibili e alla portata)
    // piu' "Annulla"; il segnalino si paga e torna nella riserva solo quando si sceglie davvero qualcosa
    public void playerAskLuckyCardBonus() {
        List<String> labels = new ArrayList<>();
        List<Runnable> effects = new ArrayList<>();
        Player player = activePlayer;
        if (warehouse.has(TileType.LEAF)) {
            labels.add(LUCKY_LEAVES + " fogliame");
            effects.add(() -> player.give(warehouse.takeLeaf(LUCKY_LEAVES)));
        }
        if (warehouse.has(TileType.WATER)) {
            labels.add(LUCKY_WATER + " acqua");
            effects.add(() -> player.give(warehouse.takeWater(LUCKY_WATER)));
        }
        if (warehouse.has(TileType.BORDER)) {
            labels.add(LUCKY_BORDER + " sponda");
            effects.add(() -> player.give(warehouse.takeBorder(LUCKY_BORDER)));
        }
        addKoiOffers(player, labels, effects, LUCKY_KOI_EXTRA);

        List<Runnable> paid = new ArrayList<>();
        for (Runnable effect : effects) {
            paid.add(() -> {
                player.payLuckyCard();
                warehouse.giveBackLuckyCard();
                effect.run();
            });
        }
        labels.add("Annulla");
        paid.add(null);
        askChoice(player, "Spendi un segnalino Fortuna per:", labels, paid);
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
        List<String> labels = new ArrayList<>();
        List<Runnable> effects = new ArrayList<>();
        addKoiOffers(activePlayer, labels, effects, -amount);
        labels.add(labels.isEmpty() ? "Nessuna carpa acquistabile: continua" : "Non compro");
        effects.add(null);
        askChoice(activePlayer, "Quale carpa compri?", labels, effects);
    }

    // carpe acquistabili: ancora sul mercato e con monete sufficienti; priceChange -1 con lo sconto, +1 con la Fortuna
    private void addKoiOffers(Player player, List<String> labels, List<Runnable> effects, int priceChange) {
        addKoiOffer(player, labels, effects, "carpa piccola", koiMarket.getSmallKoiLeft(),
                koiMarket.getSmallKoiCost() + priceChange, koiMarket::takeSmallKoi);
        addKoiOffer(player, labels, effects, "carpa media", koiMarket.getMediumKoiLeft(),
                koiMarket.getMediumKoiCost() + priceChange, koiMarket::takeMediumKoi);
        addKoiOffer(player, labels, effects, "carpa grande", koiMarket.getBigKoiLeft(),
                koiMarket.getBigKoiCost() + priceChange, koiMarket::takeBigKoi);
    }

    private void addKoiOffer(Player player, List<String> labels, List<Runnable> effects, String name, int left,
            int price, Supplier<Tile> take) {
        int cost = Math.max(0, price);
        if (left == 0 || player.getInventory().getCoins() < cost) {
            return;
        }
        labels.add("Compra una " + name + (cost == 0 ? " (gratis)" : " (" + cost + (cost == 1 ? " moneta)" : " monete)")));
        effects.add(() -> {
            player.pay(cost);
            player.give(take.get());
        });
    }

    // domanda in cui ogni opzione ha il suo effetto; null = nessun effetto (annulla, non comprare)
    private void askChoice(Player player, String question, List<String> labels, List<Runnable> effects) {
        pendingChoices.add(new PendingChoice(player, question, List.copyOf(labels), option -> {
            Runnable effect = effects.get(option);
            if (effect != null) {
                effect.run();
            }
        }));
    }

    // il magazzino si controlla solo a fine turno (regolamento pag. 7): finche' le tessere non ci stanno il giocatore
    // ne scarta una, che torna nella riserva (le carpe al mercato). true se ha messo in coda la domanda
    public boolean askStorageDiscard(Player player) {
        Inventory inventory = player.getInventory();
        if (!inventory.isOverCapacity()) {
            return false;
        }
        List<TileType> types = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (Tile tile : inventory.getTiles()) {
            TileType type = tile.getTileType();
            if (!types.contains(type)) {
                types.add(type);
                int space = tile.getSpaceOccupied();
                labels.add("Scarta: " + type.displayName() + (space > 1 ? " (" + space + " spazi)" : ""));
            }
        }
        int used = inventory.getMaxSpace() - inventory.getSpaceLeft();
        // addFirst: se serve un altro scarto arriva prima delle altre domande gia' in coda (gli obiettivi)
        pendingChoices.addFirst(new PendingChoice(player,
                "Il magazzino ha " + inventory.getMaxSpace() + " spazi ma ne stai usando " + used + ": scarta una tessera",
                List.copyOf(labels),
                option -> {
                    returnToReserve(inventory.retrieveTile(types.get(option)));
                    askStorageDiscard(player);
                }));
        return true;
    }

    private void returnToReserve(Tile tile) {
        if (tile.getTileType().isKoi()) {
            koiMarket.giveBack(tile);
        } else {
            warehouse.giveBack(tile);
        }
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

        pendingChoices.add(new PendingChoice(player,
                "Pioggia: dove piazzi questa tessera (" + type.displayName() + ")?", labels(positions),
                option -> {
                    player.getPond().place(takeFromReserve(type), positions.get(option));
                    askRainPlacement(player, rest);
                },
                singleHex(positions)));
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
        pendingChoices.add(new PendingChoice(activePlayer, "Quale preferisci?",
                List.of(money + " monete", water + " acqua"),
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


    // un giocatore esce dalla stanza prima che la partita inizi
    public void removePlayer(Player player) {
        if (players.remove(player)) {
            playerNum--;
        }
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
        closingPlayerIndex = getClosingPlayer(activePlayerIndex);
	}

    private int getClosingPlayer(int index){
        if(index>0){
            return index - 1;
        } else {
            return players.size() - 1;
        }
    }

    public void goToNextPlayer(){
        
        if(finalWorkingPhase && activePlayerIndex==closingPlayerIndex){
            over = true;   // niente giocatore successivo: il RoomController chiude la partita
            calculateScore();
            return;
        }

        if(!finalWorkingPhase && gameIsEnding && activePlayerIndex==closingPlayerIndex){
           finalWorkingPhase = true; 
        }     

        if((activePlayerIndex + 1) == playerNum){
            activePlayerIndex = 0;
        } else {
            activePlayerIndex++;
        }
        activePlayer = players.get(activePlayerIndex);
        activePlayer.isYourTurn();
    }

    // regolamento pag. 11: chi ha piu' punti vince; in caso di parita' chi ha piu' monete; se resta la parita'
    // vince chi giocava dopo nell'ordine di turno, a partire dal primo giocatore
	private void calculateScore() {
        ScoreCalculator calculator = new ScoreCalculator();
        int first = (closingPlayerIndex + 1) % playerNum;   // il giocatore di chiusura siede alla destra del primo
        List<ScoreSheet> inTurnOrder = new ArrayList<>();
        for (int i = 0; i < playerNum; i++) {
            inTurnOrder.add(calculator.score(players.get((first + i) % playerNum)));
        }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < inTurnOrder.size(); i++) {
            order.add(i);
        }
        order.sort(Comparator.<Integer>comparingInt(i -> inTurnOrder.get(i).total())
                .thenComparingInt(i -> inTurnOrder.get(i).coins())
                .thenComparingInt(i -> i)      // a parita' vince chi giocava dopo
                .reversed());
        List<ScoreSheet> ranking = new ArrayList<>();
        for (int i : order) {
            ranking.add(inTurnOrder.get(i));
        }
        finalScores = List.copyOf(ranking);
    }

    public List<ScoreSheet> getFinalScores() {
        return finalScores;
    }

    public boolean isOver() {
        return over;
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
                "Hai piazzato una carpa: vuoi una tessera Percorso per " + ObjectiveType.PATH.cost() + " monete?",
                List.of("Prendi il Percorso", "No, grazie"),
                option -> {
                    if (option == 0) {
                        askPathPlacement(player, path.claim(player));
                    }
                }));
    }

    // gli obiettivi vanno piazzati subito: domanda con coordinate, cosi' nella GUI si clicca lo stagno
    private void askPathPlacement(Player player, Tile tile) {
        List<HexPosition> positions = player.getPlayablePositions(TileType.PATH);
        pendingChoices.add(new PendingChoice(player, "Dove piazzi il Percorso?", labels(positions),
                option -> player.getPond().placePath(tile, positions.get(option)),
                singleHex(positions)));
    }

    // domande di piazzamento: le coordinate come testo ("(1,-2)") e, per il client, un esagono per opzione
    private static List<String> labels(List<HexPosition> positions) {
        List<String> labels = new ArrayList<>();
        for (HexPosition position : positions) {
            labels.add(position.toString());
        }
        return labels;
    }

    private static List<Placement> singleHex(List<HexPosition> positions) {
        List<Placement> placements = new ArrayList<>();
        for (HexPosition position : positions) {
            placements.add(new Placement(position, 0, List.of(position)));
        }
        return placements;
    }

    // obiettivi da piazzare nello stagno (rami, shishi odoshi, carpa centenaria, tartaruga, ninfea viola, ponticello,
    // statua, cascata, salice): una sola domanda, un'opzione per ogni posizione e rotazione valida, ciascuna con
    // gli esagoni che occuperebbe. Nel client si clicca lo stagno con l'anteprima, come per la sponda; infine "then"
    private void askObjectivePlacement(Player player, Tile tile, Runnable then) {
        TileType type = tile.getTileType();
        List<String> labels = new ArrayList<>();
        List<Placement> placements = new ArrayList<>();
        for (HexPosition position : player.getPlayablePositions(type)) {
            List<Integer> rotations = player.validRotations(type, position);
            for (int rotation : rotations) {
                labels.add(position + (rotations.size() == 1 ? ""
                        : " rotazione " + (rotation % 6) + (rotation >= 6 ? " (specchiata)" : "")));
                placements.add(new Placement(position, rotation, player.getPond().footprint(type, position, rotation)));
            }
        }
        if (placements.isEmpty()) {
            then.run();   // non dovrebbe succedere: il requisito garantisce un posto (la tessera resta registrata da claim)
            return;
        }
        pendingChoices.add(new PendingChoice(player, "Dove piazzi questa tessera (" + type.displayName() + ")?", labels,
                option -> {
                    Placement chosen = placements.get(option);
                    player.getPond().placeObjective(tile, chosen.anchor(), chosen.rotation());
                    then.run();
                },
                placements));
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
        options.add("No, grazie");

        pendingChoices.add(new PendingChoice(player, "Puoi reclamare un obiettivo: lo vuoi?", options,
                option -> {
                    if (option == claimable.size()) {
                        return;                                   // "No, grazie"
                    }
                    Tile tile = claimable.get(option).claim(player);
                    // magari ne puo' prendere un altro: si ricontrolla solo dopo il piazzamento, che toglie acqua libera
                    if (placedByShape(tile.getTileType())) {
                        askObjectivePlacement(player, tile, () -> checkObjectivesforPlayer(player));
                    } else {
                        checkObjectivesforPlayer(player);   // la tessera e' gia' registrata nel giocatore da claim
                    }
                }));
        return true;
    }

    @Override
    public void noMoreCards() {
        gameIsEnding = true;
    }
    
    @Override
    public void noMoreKoi() {
        gameIsEnding = true;    
    }

    public boolean isFinalTurn() {
        return finalWorkingPhase;
    }





}
