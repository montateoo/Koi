package server;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import network.CellView;
import network.MatchRecord;
import network.GameView;
import network.HexCoord;
import network.ObjectiveView;
import network.PlacementView;
import network.PlayerView;
import network.RoomInfo;
import game.Cell;
import game.GameRoom;
import game.HexPosition;
import game.Inventory;
import game.KoiMarket;
import game.PendingChoice;
import game.Placement;
import game.Player;
import game.PlayerState;
import game.Tile;
import game.TileType;
import game.cards.Actions;
import game.objectives.Objective;
import game.objectives.ObjectiveType;
import game.scoring.ScoreSheet;
import messages.ServerMessage;
import game.cards.Card;


public class RoomController {

    private String gameId;
    private String roomName;
    private GameRoom game;
    private GameState gameState;
    private LinkedHashMap<Player, ClientHandler> joinedPlayers;
    private final Set<Player> readyPlayers = new HashSet<>();
    private Player endingTurn;   // giocatore che ha premuto End ma deve ancora rispondere sugli obiettivi
    private final Consumer<MatchRecord> onFinished;   // a fine partita: la lobby toglie la stanza e la mette nello storico
    private final Runnable onAbandoned;               // non c'e' piu' nessuno: la lobby toglie la stanza
    private final Set<Player> disconnected = new HashSet<>();   // giocatori in partita la cui connessione e' caduta


    public RoomController(String id, String roomName, Consumer<MatchRecord> onFinished, Runnable onAbandoned){
        this.gameId = id;
        this.roomName = roomName;
        this.gameState = GameState.WAITING;
        this.game = new GameRoom();
        this.joinedPlayers = new LinkedHashMap<>();
        this.onFinished = onFinished;
        this.onAbandoned = onAbandoned;
    }

    // il giocatore esce dalla stanza: si puo' in attesa (libera il posto) e a partita finita (torna al menu);
    // durante la partita no, finche' non decidiamo come continuare senza di lui. false se l'uscita e' rifiutata
    public synchronized boolean removePlayer(Player player) {
        if (gameState == GameState.PLAYING) {
            return false;
        }
        joinedPlayers.remove(player);
        if (gameState == GameState.WAITING) {
            game.removePlayer(player);
            readyPlayers.remove(player);
            sendMessageToAll(player.getName() + " ha lasciato la stanza");
            if (joinedPlayers.isEmpty()) {
                onAbandoned.run();   // stanza in attesa rimasta vuota: non resta nella lista
            }
        }
        return true;
    }

    // la connessione e' caduta (pagina chiusa, rete persa). In attesa o a fine partita equivale a uscire;
    // durante la partita il giocatore resta in gioco, ma se si scollegano tutti la stanza non serve piu'
    public synchronized void disconnect(Player player) {
        if (removePlayer(player)) {
            return;
        }
        disconnected.add(player);
        sendMessageToAll(player.getName() + " si è scollegato");
        if (disconnected.size() == joinedPlayers.size()) {
            onAbandoned.run();
        }
    }

    // true se il giocatore e' entrato; e' la stanza a dire Joined, cosi' arriva prima del benvenuto
    public synchronized boolean addPlayer(Player newPlayer, ClientHandler client) {
        if (gameState != GameState.WAITING) {
            client.send(new ServerMessage.ErrorMessage("La partita è già iniziata"));
            return false;
        }
        if (game.isFull()) {
            client.send(new ServerMessage.ErrorMessage("La stanza è piena"));
            return false;
        }
        // il client riconosce se stesso dal nome (GameView.viewer): due nomi uguali lo confonderebbero
        for (Player other : joinedPlayers.keySet()) {
            if (other.getName().equalsIgnoreCase(newPlayer.getName())) {
                client.send(new ServerMessage.ErrorMessage("Nella stanza c'è già un giocatore di nome " + other.getName()));
                return false;
            }
        }
        joinedPlayers.put(newPlayer, client);
        game.addPlayer(newPlayer);
        client.send(new ServerMessage.Joined(gameId, roomName));
        sendMessageToAll(newPlayer.getName() + " è entrato nella stanza");
        return true;
    }

    // la riga della stanza nella lista della lobby
    public synchronized RoomInfo info() {
        List<String> names = new ArrayList<>();
        for (Player player : joinedPlayers.keySet()) {
            names.add(player.getName());
        }
        return new RoomInfo(gameId, roomName, names, gameState == GameState.WAITING && !game.isFull());
    }

    public synchronized void startGame(Player player) {
        if (gameState != GameState.WAITING) {
            tell(player, "La partita è già iniziata");
            return;
        }
        readyPlayers.add(player);
        sendMessageToAll(player.getName() + " è pronto (" + readyPlayers.size() + "/" + joinedPlayers.size() + ")");

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
            tell(player, "Scegli una posizione tra 0 e " + (boardSize - 1));
            return;
        }
        if (game.getBoard().getDrawableCards().get(position) == null) {
            tell(player, "Quello spazio è vuoto: il mazzo è finito");
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
        for (Map.Entry<Player, ClientHandler> entry : joinedPlayers.entrySet()) {
            entry.getValue().send(new ServerMessage.GameUpdate(buildView(entry.getKey())));
        }
    }

    private GameView buildView(Player viewer) {
        List<String> board = new ArrayList<>();
        for (Card card : game.getBoard().getDrawableCards()) {
            board.add(card == null ? "" : card.describe());   // "" = spazio vuoto (mazzo finito)
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
        List<PlacementView> placements = new ArrayList<>();
        if (pending != null) {
            if (pending.player() == viewer) {
                question = pending.question();
                options = pending.options();
                for (int option = 0; option < pending.placements().size(); option++) {
                    Placement placement = pending.placements().get(option);
                    List<HexCoord> cells = new ArrayList<>();
                    for (HexPosition cell : placement.cells()) {
                        cells.add(new HexCoord(cell.q(), cell.r()));
                    }
                    placements.add(new PlacementView(option, new HexCoord(placement.anchor().q(), placement.anchor().r()),
                            placement.rotation(), cells));
                }
            } else {
                question = "In attesa della scelta di " + pending.player().getName();
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

        // carpe gia' nello stagno: le destinazioni dipendono dalla carpa (gli esagoni che lascia tornano liberi)
        if (viewer == active && pending == null) {
            for (int koiId : viewer.koiIds()) {
                List<HexCoord> anchors = new ArrayList<>();
                Map<HexCoord, List<Integer>> byAnchor = new LinkedHashMap<>();
                for (Map.Entry<HexPosition, List<Integer>> move : viewer.koiMoves(koiId).entrySet()) {
                    HexCoord anchor = new HexCoord(move.getKey().q(), move.getKey().r());
                    anchors.add(anchor);
                    byAnchor.put(anchor, move.getValue());
                }
                if (!anchors.isEmpty()) {
                    playable.put(GameView.moveKey(koiId), anchors);
                    rotations.put(GameView.moveKey(koiId), byAnchor);
                }
            }
        }

        boolean canDraw = viewer == active && pending == null && viewer.getState() == PlayerState.IDLE
                && gameState == GameState.PLAYING && game.getBoard().hasCards();

        // i tre obiettivi sul tavolo; "claimable" solo per quelli che si reclamano a fine turno (non il Percorso)
        List<ObjectiveView> objectives = new ArrayList<>();
        for (Objective objective : game.getObjectives()) {
            ObjectiveType type = objective.getObjectiveType();
            TileType preview = objective.previewTileType(viewer);
            objectives.add(new ObjectiveView(type.name(), type.displayName(), type.requirement(), type.cost(),
                    objective.tilesLeft(), preview == null ? null : preview.name(),
                    type != ObjectiveType.PATH && objective.canBeClaimedBy(viewer)));
        }

        return new GameView(viewer.getName(), active == null ? null : active.getName(), board,
            market.getSmallKoiLeft(), market.getMediumKoiLeft(), market.getBigKoiLeft(), players,
            question, options, playable, rotations, canDraw, objectives, placements);
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
        for(ClientHandler target: joinedPlayers.values()){
            target.send(new ServerMessage.Info(message));
        }
    }

    private void tell(Player target, String message){
        joinedPlayers.get(target).send(new ServerMessage.Info(message));
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
                    if (player.equals(endingTurn) && game.getPendingChoice() == null) {
                        endingTurn = null;
                        finishTurn(player);      // ultima risposta data: il turno passa
                        return;
                    }
                    sendToAllPlayer();
                } else {
                    tell(player, "Scelta non valida");
                    return;                  
                }
            } else {
                tell(player, "Tocca a " + choice.player().getName() + " scegliere");
                return;
            }
        } else {
            tell(player, "Non c'è niente da scegliere adesso");
            return;  
        }
    }

    // il giocatore vuole lavorare: va bene se e' IDLE o sta gia' lavorando
    private boolean startWorking(Player player) {
        if (player.getState() == PlayerState.MEDITATING) {
            tell(player, "Hai preso una carta in questo turno: non puoi lavorare");
            return false;
        }
        player.working();
        return true;
    }

    private boolean startMeditating(Player player) {
        if (player.getState() == PlayerState.WORKING) {
            tell(player, "Stai lavorando in questo turno: non puoi prendere carte");
            return false;
        }
        if (player.getState() == PlayerState.MEDITATING) {
            tell(player, "Hai già preso una carta in questo turno");
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
            tell(player, "Nel magazzino non hai: " + tileType.displayName());
            return;
        }
        if (!player.getPlayablePositions(tileType).contains(position)) {
            tell(player, "Qui non si può piazzare: " + tileType.displayName());
            return;
        }
        if (!player.hasWorkerFor(action)) {
            tell(player, "Nessun lavoratore libero per piazzare: " + tileType.displayName());
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
            tell(player, "Nel magazzino non hai: " + tileType.displayName());
            return;
        }
        if (!player.canPlace(tileType, position, angle)) {
            tell(player, "Qui non si può piazzare con quella rotazione: " + tileType.displayName());
            return;
        }
        if (!player.hasWorkerFor(action)) {
            tell(player, "Nessun lavoratore libero per piazzare: " + tileType.displayName());
            return;
        }
        if (!startWorking(player)) {   // ultimo controllo: da qui lo stato cambia
            return;
        }

        if (tileType == TileType.BORDER) {
            player.placeBorder(action, position, angle);
        } else if (tileType.isKoi()) {
            player.placeKoi(tileType, action, position, angle);
            game.offerPath(player);
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
            tell(player, "Nella riserva non c'è più: " + tileType.displayName());
            return;
        }
        if (!player.getPlayablePositions(tileType).contains(position)) {
            tell(player, "Qui non si può piazzare: " + tileType.displayName());
            return;
        }
        if (!player.hasWorkerFor(action)) {
            tell(player, "Nessun lavoratore libero per piazzare: " + tileType.displayName());
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

    private void moveKoi(Player player, Actions action, int koiId, int q, int r, int angle) {
        if (!canAct(player)) {
            return;
        }
        HexPosition position = new HexPosition(q, r);
        if (!player.hasKoi(koiId)) {
            tell(player, "Nel tuo stagno non c'è la carpa " + koiId);
            return;
        }
        if (player.isKoiAt(koiId, position, angle)) {
            tell(player, "La carpa è già lì: scegli un altro posto");
            return;
        }
        if (!player.canMoveKoi(koiId, position, angle)) {
            tell(player, "La carpa non entra lì con quella rotazione");
            return;
        }
        if (!player.hasWorkerFor(action)) {
            tell(player, "Nessun lavoratore libero per spostare la carpa");
            return;
        }
        if (!startWorking(player)) {   // ultimo controllo: da qui lo stato cambia
            return;
        }
        player.moveKoi(action, koiId, position, angle);
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
            tell(player, "Il tipo di carpa deve essere 0 (piccola), 1 (media) o 2 (grande)");
        } 
    }

    public synchronized void moveKoi(Player player, int koiId, int q, int r, int angle) {
        moveKoi(player, Actions.MOVEKOI, koiId, q, r, angle);
    }

    private boolean canAct(Player player){
        if (gameState == GameState.FINISHED) {
            tell(player, "La partita è finita");
            return false;
        }
        if (gameState != GameState.PLAYING) {
            tell(player, "La partita non è ancora iniziata");
            return false;
        }
        if (player != game.getActivePlayer()) {
            tell(player, "Non è il tuo turno: tocca a " + game.getActivePlayer().getName());
            return false ;
        }
        if (game.getPendingChoice() != null) {
            if(game.getPendingChoice().player() == player){
                tell(player, "Rispondi prima alla domanda");    
            } else {
                tell(player, "In attesa della scelta di " + game.getPendingChoice().player().getName());
                
            }
            return false; 
        }
        return true;
    }

    public synchronized void endTurn(Player player) {
        if (!canAct(player)) {
            return;
        }
        // tabellone vuoto (mazzo finito e carte esaurite): chi non ha niente da piazzare deve poter passare,
        // altrimenti la partita si blocca
        if (player.getState() == PlayerState.IDLE && game.getBoard().hasCards()) {
            tell(player, "Prima di passare il turno devi meditare (prendere una carta) o lavorare");
            return;
        }
        // fine turno: prima gli scarti se il magazzino e' troppo pieno, poi gli obiettivi;
        // il turno passa quando sono finite le risposte (vedi choose)
        boolean questions = game.askStorageDiscard(player);
        questions |= game.checkObjectivesforPlayer(player);
        if (questions) {
            endingTurn = player;
            sendToAllPlayer();       // mostra la domanda
            return;
        }
        finishTurn(player);
    }

    private void finishTurn(Player player) {
        player.turnEnded();          // stato IDLE, giocatore e aiutanti si riposano
        game.goToNextPlayer();
        if (game.isOver()) {         // era l'ultimo turno del giro finale
            changeState();           // PLAYING -> FINISHED: da qui canAct rifiuta ogni azione
            sendToAllPlayer();       // l'ultimo stato dello stagno, poi la classifica sopra
            List<ScoreSheet> ranking = game.getFinalScores();
            sendMessageToAll("Partita finita! Vince " + ranking.get(0).name() + " con " + ranking.get(0).total() + " punti");
            for (ClientHandler client : joinedPlayers.values()) {
                client.send(new ServerMessage.GameOver(ranking));
            }
            // nello storico: classifica e fotografia finale di ogni stagno
            List<PlayerView> finalPonds = new ArrayList<>();
            for (Player p : game.getPlayers()) {
                finalPonds.add(buildPlayerView(p));
            }
            String endedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            onFinished.accept(new MatchRecord(roomName, endedAt, ranking, finalPonds));
            return;
        }
        sendMessageToAll("Tocca a " + game.getActivePlayer().getName());
        sendToAllPlayer();
    }

	public synchronized void spendLuckyCard(Player player) {
        if(!canAct(player)){
            return;
        }

        if (!player.getInventory().hasLuckyCard()) {
            tell(player, "Non hai segnalini Fortuna");
            return;
        }

        game.playerAskLuckyCardBonus();
        sendToAllPlayer();
    }


    
}
