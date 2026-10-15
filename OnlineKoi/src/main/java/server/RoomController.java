package server;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import network.CellView;
import network.ClientCallback;
import network.GameView;
import network.HexCoord;
import network.PlayerView;
import game.Cell;
import game.GameRoom;
import game.HexPosition;
import game.Inventory;
import game.KoiMarket;
import game.PendingChoice;
import game.Player;
import game.PlayerState;
import game.Tile;
import game.TileType;
import game.cards.Actions;
import game.cards.Card;


public class RoomController {

    private String gameId;
    private String roomName;
    private GameRoom game;
    private GameState gameState;
    private LinkedHashMap<Player, ClientCallback> joinedPlayers;
    private final Set<Player> readyPlayers = new HashSet<>();
    
    public RoomController(String id, String roomName){
        this.gameId = id;
        this.roomName = roomName;
        this.gameState = GameState.WAITING;
        this.game = new GameRoom();
        this.joinedPlayers = new LinkedHashMap<>();
    }

    public synchronized void addPlayer(Player newPlayer, ClientCallback callback){
        if((gameState==GameState.WAITING)&&(!game.isFull())){
            joinedPlayers.put(newPlayer, callback);
            game.addPlayer(newPlayer);          
            try {
                callback.onMessage("Welcome to the game " + newPlayer.getName());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            //TODO handle that the room is closed
        }
    }

    public synchronized void startGame(Player player) {
        if (gameState != GameState.WAITING) {
            tell(player, "The game has already started");
            return;
        }
        readyPlayers.add(player);
        sendMessageToAll(player.getName() + " is ready (" + readyPlayers.size() + "/" + joinedPlayers.size() + ")");

        if (readyPlayers.size() == joinedPlayers.size()) {
            gameState = GameState.PLAYING;
            game.prepareGame();
            sendToAllPlayer();
        }
    }

    public synchronized void drawCard(Player player, int position){
        if(!canAct(player)){
            return;
        }

        int boardSize = game.getBoard().getDrawableCards().size();
        if (position < 0 || position >= boardSize) {
            tell(player, "Choose a position between 0 and " + (boardSize - 1));
            return;
        }
        
        if (!startMeditating(player)) { // qui il giocatore diventa MEDITATING
            return;
        }

        game.onCardDrawn(position);
        sendToAllPlayer();
    }

    // ogni giocatore riceve la propria GameView, cosi' il client sa chi e' "you"
    private void sendToAllPlayer() {
        for(Map.Entry<Player, ClientCallback> entry: joinedPlayers.entrySet()){
            try {
                entry.getValue().onGameUpdate(buildView(entry.getKey()));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private GameView buildView(Player viewer) {
        List<String> board = new ArrayList<>();
        for (Card card : game.getBoard().getDrawableCards()) {
            board.add(card.describe());
        }

        List<PlayerView> players = new ArrayList<>();
        for (Player p : game.getPlayers()) {
            players.add(buildPlayerView(p));
        }

        Player active = game.getActivePlayer();
        KoiMarket market = game.getMarket();

        PendingChoice pending = game.getPendingChoice();
        String question = null;
        List<String> options = List.of();
        if (pending != null) {
            if (pending.player() == viewer) {
                question = pending.question();
                options = pending.options();
            } else {
                question = "Waiting for " + pending.player().getName() + " to choose";
            }
        }

        Map<String, List<HexCoord>> playable = new LinkedHashMap<>();
        if (viewer == active && pending == null) {
            Set<TileType> owned = new LinkedHashSet<>();
            for (Tile tile : viewer.getInventory().getTiles()) {
                owned.add(tile.getTileType());
            }
            for (TileType upgrade : List.of(TileType.LILY, TileType.LANTERN)) {
                if (game.reserveHas(upgrade)) {
                    owned.add(upgrade);
                }
            }
            for (TileType type : owned) {
                List<HexCoord> coords = new ArrayList<>();
                for (HexPosition position : viewer.getPlayablePositions(type)) {
                    coords.add(new HexCoord(position.q(), position.r()));
                }
                if (!coords.isEmpty()) {
                    playable.put(type.name(), coords);
                }
            }
        }

        // le tessere su piu' esagoni hanno bisogno anche della rotazione: per ogni ancoraggio valido, quelle ammesse
        Map<String, Map<HexCoord, List<Integer>>> rotations = new LinkedHashMap<>();
        for (TileType type : List.of(TileType.BORDER, TileType.MEDIUMKOI, TileType.BIGKOI)) {
            Map<HexCoord, List<Integer>> byAnchor = new LinkedHashMap<>();
            for (HexCoord anchor : playable.getOrDefault(type.name(), List.of())) {
                byAnchor.put(anchor, viewer.validRotations(type, new HexPosition(anchor.q(), anchor.r())));
            }
            if (!byAnchor.isEmpty()) {
                rotations.put(type.name(), byAnchor);
            }
        }

        boolean canDraw = viewer == active && pending == null && viewer.getState() == PlayerState.IDLE;

        return new GameView(viewer.getName(), active == null ? null : active.getName(), board,
            market.getSmallKoiLeft(), market.getMediumKoiLeft(), market.getBigKoiLeft(), players,
            question, options, playable, rotations, canDraw);
    }

    private PlayerView buildPlayerView(Player player) {
        Inventory inventory = player.getInventory();

        List<String> tiles = new ArrayList<>();
        for (Tile tile : inventory.getTiles()) {
            tiles.add(tile.getTileType().name());
        }

        return new PlayerView(player.getName(), inventory.getCoins(), inventory.getLuckyCards(),
                inventory.getMaxSpace() - inventory.getSpaceLeft(), inventory.getMaxSpace(),
                tiles, describeAll(inventory.getHelpers()), describeAll(inventory.getCards()),
                describeAll(inventory.getScrolls()), buildPondView(player));
    }

    // lo stagno del giocatore convertito in dati che attraversano la rete: un CellView per esagono occupato
    private List<CellView> buildPondView(Player player) {
        List<CellView> pond = new ArrayList<>();
        for (Map.Entry<HexPosition, Cell> entry : player.getPond().getCells().entrySet()) {
            Cell cell = entry.getValue();
            List<Boolean> sides = new ArrayList<>();
            for (int side = 0; side < 6; side++) {
                sides.add(cell.isBlue(side));
            }
            pond.add(new CellView(entry.getKey().q(), entry.getKey().r(),
                    cell.getBase().getTileType().name(),
                    cell.getTop() == null ? null : cell.getTop().getTileType().name(),
                    cell.getTopRotation(),
                    cell.getBase().getTileID(),
                    cell.getTop() == null ? -1 : cell.getTop().getTileID(),
                    sides));
        }
        return pond;
    }

    private static List<String> describeAll(List<? extends Card> cards) {
        List<String> descriptions = new ArrayList<>();
        for (Card card : cards) {
            descriptions.add(card.describe());
        }
        return descriptions;
    }

    private void sendMessageToAll(String message){
        for(ClientCallback target: joinedPlayers.values()){
            try {
                target.onMessage(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void tell(Player target, String message){
        try {
            joinedPlayers.get(target).onMessage(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private synchronized void changeState() {
        if(gameState==GameState.WAITING){
            gameState = GameState.PLAYING;
        } else if (gameState==GameState.PLAYING){
            gameState = GameState.FINISHED;
        }
    }

    public synchronized void choose (Player player, int option){

        PendingChoice choice = game.getPendingChoice();

        if(choice != null){
            if(player.equals(choice.player())){
                if((option>=0) &&(choice.options().size()>option)){
                    game.answerChoice(option);
                    sendToAllPlayer();
                } else {
                    tell(player, "Choice is out of the range");
                    return;                  
                }
            } else {
                tell(player, "It's " + choice.player().getName()+ "'s choice");
                return;
            }
        } else {
            tell(player, "There is nothing to choose right now");
            return;  
        }
    }

    // il giocatore vuole lavorare: va bene se e' IDLE o sta gia' lavorando
    private boolean startWorking(Player player) {
        if (player.getState() == PlayerState.MEDITATING) {
            tell(player, "You drew a card this turn, you can't work");
            return false;
        }
        player.working();
        return true;
    }

    private boolean startMeditating(Player player) {
        if (player.getState() == PlayerState.WORKING) {
            tell(player, "You are working this turn, you can't draw");
            return false;
        }
        if (player.getState() == PlayerState.MEDITATING) {
            tell(player, "You already took a card this turn");
            return false;
        }
        player.meditating();
        return true;
    }

    private void placeTile(Player player, TileType tileType, Actions action, int q, int r) {
        if(!canAct(player)){
            return;
        }

        HexPosition position = new HexPosition(q, r);
        if (!player.getInventory().hasTile(tileType)) {
            tell(player, "You have no " + tileType.displayName() + " in your storage");
            return;
        }
        if (!player.getPlayablePositions(tileType).contains(position)) {
            tell(player, "You can't place the " + tileType.displayName() + " there");
            return;
        }
        if (!player.hasWorkerFor(action)) {
            tell(player, "Nobody is free to place the " + tileType.displayName());
            return;
        }
        if (!startWorking(player)) {   // ultimo controllo: da qui lo stato cambia
            return;
        }
        player.placeTile(tileType, action, position);
        sendToAllPlayer();
    }

    private void placeTile(Player player, TileType tileType, Actions action, int q, int r, int angle) {
        if(!canAct(player)){
            return;
        }

        HexPosition position = new HexPosition(q, r);
        if (!player.getInventory().hasTile(tileType)) {
            tell(player, "You have no " + tileType.displayName() + " in your storage");
            return;
        }
        if (!player.canPlace(tileType, position, angle)) {
            tell(player, "You can't place the " + tileType.displayName() + " there with that rotation");
            return;
        }
        if (!player.hasWorkerFor(action)) {
            tell(player, "Nobody is free to place the " + tileType.displayName());
            return;
        }
        if (!startWorking(player)) {   // ultimo controllo: da qui lo stato cambia
            return;
        }

        if (tileType == TileType.BORDER) {
            player.placeBorder(action, position, angle);
        } else if (tileType.isKoi()) {
            player.placeKoi(tileType, action, position, angle);
        } else {
            throw new IllegalStateException("Unexpected tile type " + tileType);
        }

        sendToAllPlayer();
    }

    private void upgradeTile(Player player, TileType tileType, Actions action, int q, int r) {
        if(!canAct(player)){
            return;
        }
        
        HexPosition position = new HexPosition(q, r);
        if (!game.reserveHas(tileType)) {
            tell(player, "There is no " + tileType.displayName() + " left in the reserve");
            return;
        }
        if (!player.getPlayablePositions(tileType).contains(position)) {
            tell(player, "You can't place the " + tileType.displayName() + " there");
            return;
        }
        if (!player.hasWorkerFor(action)) {
            tell(player, "Nobody is free to place the " + tileType.displayName());
            return;
        }
        if (!startWorking(player)) {
            return;
        }
        player.upgradeTile(action, position, game.takeFromReserve(tileType));   // solo ora si prende dalla riserva
        if (tileType == TileType.LANTERN) {
            player.giveLuckyCard();
        }
        sendToAllPlayer();
    }

    public synchronized void placeWater(Player player, int q, int r) {
        placeTile(player, TileType.WATER, Actions.PLACEWATER, q, r);
    }

    public synchronized void placeBorder(Player player, int q, int r, int angle) {
        placeTile(player, TileType.BORDER, Actions.PLACEBORDER, q, r, angle);
    }

    public synchronized void placeLantern(Player player, int q, int r) {
        upgradeTile(player, TileType.LANTERN, Actions.PLACELANTERN, q, r);
    }

    public synchronized void placeLeafs(Player player, int q, int r) {
        placeTile(player, TileType.LEAF, Actions.PLACELEAFS, q, r);
    }

    public synchronized void placeLily(Player player, int q, int r) {
        upgradeTile(player, TileType.LILY, Actions.PLACELILY, q, r);
    }

    public synchronized void placeKoi(Player player, int type, int q, int r, int angle) {
        if(type==0){
            placeTile(player, TileType.SMALLKOI, Actions.PLACEKOI, q, r, angle);
        } else if (type==1) {
            placeTile(player, TileType.MEDIUMKOI, Actions.PLACEKOI, q, r, angle);
        } else if (type==2) {
            placeTile(player, TileType.BIGKOI, Actions.PLACEKOI, q, r, angle);
        } else {
            tell(player, "Koi type must be 0 (small), 1 (medium) or 2 (big)");
        } 
    }

    public synchronized void moveKoi(Player player) {
        tell(player, "Moving koi is not available yet");
    }

    private boolean canAct(Player player){
        if (gameState != GameState.PLAYING) {
            tell(player, "The game has not started yet");
            return false;
        }
        if (player != game.getActivePlayer()) {
            tell(player, "It's not your turn, wait for " + game.getActivePlayer().getName());
            return false ;
        }
        if (game.getPendingChoice() != null) {
            if(game.getPendingChoice().player() == player){
                tell(player, "Answer the question first (choose <number>)");    
            } else {
                tell(player, "Waiting for " + game.getPendingChoice().player().getName() + " to choose");
                
            }
            return false; 
        }
        return true;
    }

    public synchronized void endTurn(Player player) {
        if (!canAct(player)) {
            return;
        }
        if (player.getState() == PlayerState.IDLE) {
            tell(player, "You must meditate (draw a card) or work before ending your turn");
            return;
        }
        player.turnEnded();          // stato IDLE, giocatore e aiutanti si riposano
        game.goToNextPlayer();
        sendMessageToAll("It's " + game.getActivePlayer().getName() + "'s turn");
        sendToAllPlayer();
    }

    
}
