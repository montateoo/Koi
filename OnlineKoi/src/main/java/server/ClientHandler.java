package server;

import java.util.function.Consumer;

import game.Player;
import messages.ClientMessage;
import messages.ServerMessage;

// un client collegato: riceve i suoi messaggi dal trasporto e li smista alla lobby o alla sua stanza.
// Non sa nulla di socket o WebSocket: le risposte partono dalla Connection
public class ClientHandler {

    static final int MAX_NAME = 20;        // nome del giocatore
    static final int MAX_ROOM_NAME = 30;   // nome della stanza

    private final Lobby lobby;
    private final Connection connection;
    private Player player;          // null finche' il client non entra in una stanza
    private RoomController room;    // idem

    public ClientHandler(Lobby lobby, Connection connection) {
        this.lobby = lobby;
        this.connection = connection;
    }

    public void send(ServerMessage message) {
        connection.send(message);
    }

    // un messaggio alla volta per client: il trasporto li consegna in ordine
    public void handle(ClientMessage message) {
        try {
            dispatch(message);
        } catch (RuntimeException e) {
            // un errore nel codice del gioco non deve far cadere la connessione
            send(new ServerMessage.ErrorMessage("Errore del server: " + e.getMessage()));
        }
    }

    // la connessione e' caduta (o il client ha chiuso la pagina): la stanza decide cosa farne
    public void disconnected() {
        if (room != null) {
            room.disconnect(player);
            room = null;
            player = null;
        }
    }

    private void dispatch(ClientMessage message) {
        switch (message) {
            // lobby: sempre permessi
            case ClientMessage.ListRooms() -> send(new ServerMessage.RoomList(lobby.listRooms()));
            case ClientMessage.CreateRoom(String name) -> {
                if (validRoomName(name)) {
                    lobby.createRoom(name.trim());
                    send(new ServerMessage.Info("Stanza \"" + name.trim() + "\" creata"));
                    send(new ServerMessage.RoomList(lobby.listRooms()));
                }
            }
            case ClientMessage.JoinRoom(String roomId, String nickname) -> join(roomId, nickname);
            case ClientMessage.CreateAndJoinRoom(String name, String nickname) -> {
                if (validRoomName(name) && validNickname(nickname) && notInRoom()) {
                    join(lobby.createRoom(name.trim()), nickname);
                }
            }
            case ClientMessage.Leave() -> leaveRoom();
            case ClientMessage.ListHistory() -> send(new ServerMessage.History(lobby.history()));

            // stanza: solo dopo essere entrati
            case ClientMessage.StartGame() -> inRoom(r -> r.startGame(player));
            case ClientMessage.DrawCard(int position) -> inRoom(r -> r.drawCard(player, position));
            case ClientMessage.Choose(int option) -> inRoom(r -> r.choose(player, option));
            case ClientMessage.EndTurn() -> inRoom(r -> r.endTurn(player));
            case ClientMessage.PlaceWater(int q, int r) -> inRoom(room -> room.placeWater(player, q, r));
            case ClientMessage.PlaceLeafs(int q, int r) -> inRoom(room -> room.placeLeafs(player, q, r));
            case ClientMessage.PlaceLily(int q, int r) -> inRoom(room -> room.placeLily(player, q, r));
            case ClientMessage.PlaceLantern(int q, int r) -> inRoom(room -> room.placeLantern(player, q, r));
            case ClientMessage.PlaceBorder(int q, int r, int angle) -> inRoom(room -> room.placeBorder(player, q, r, angle));
            case ClientMessage.PlaceKoi(int size, int q, int r, int angle) -> inRoom(room -> room.placeKoi(player, size, q, r, angle));
            case ClientMessage.MoveKoi(int koiId, int q, int r, int angle) -> inRoom(room -> room.moveKoi(player, koiId, q, r, angle));
            case ClientMessage.SpendLuckyCard() -> inRoom(room -> room.spendLuckyCard(player));
        }
    }

    private void join(String roomId, String nickname) {
        if (!notInRoom() || !validNickname(nickname)) {
            return;
        }
        RoomController chosen = roomId == null ? null : lobby.getRoom(roomId);
        if (chosen == null) {
            send(new ServerMessage.ErrorMessage("Questa stanza non esiste più"));
            return;
        }

        Player newPlayer = new Player(nickname.trim());
        if (chosen.addPlayer(newPlayer, this)) {   // Joined lo manda la stanza
            player = newPlayer;
            room = chosen;
        }
    }

    // i nomi arrivano da un browser: niente vuoti, niente lunghissimi
    private boolean validNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            send(new ServerMessage.ErrorMessage("Scegli un nome prima di entrare"));
            return false;
        }
        if (nickname.trim().length() > MAX_NAME) {
            send(new ServerMessage.ErrorMessage("Il nome può avere al massimo " + MAX_NAME + " caratteri"));
            return false;
        }
        return true;
    }

    private boolean validRoomName(String name) {
        if (name == null || name.isBlank()) {
            send(new ServerMessage.ErrorMessage("Scrivi un nome per la stanza"));
            return false;
        }
        if (name.trim().length() > MAX_ROOM_NAME) {
            send(new ServerMessage.ErrorMessage("Il nome della stanza può avere al massimo " + MAX_ROOM_NAME + " caratteri"));
            return false;
        }
        return true;
    }

    private boolean notInRoom() {
        if (room != null) {
            send(new ServerMessage.ErrorMessage("Sei già in una stanza"));
            return false;
        }
        return true;
    }

    private void inRoom(Consumer<RoomController> action) {
        if (room == null) {
            send(new ServerMessage.ErrorMessage("Prima entra in una stanza"));
        } else {
            action.accept(room);
        }
    }

    // esce dalla stanza restando collegato: il client torna al menu e puo' entrare in un'altra stanza
    private void leaveRoom() {
        if (room == null) {
            return;
        }
        if (!room.removePlayer(player)) {
            send(new ServerMessage.ErrorMessage("Non puoi uscire mentre la partita è in corso"));
            return;
        }
        room = null;
        player = null;
    }
}
