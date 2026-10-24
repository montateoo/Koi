package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

import game.Player;
import messages.ClientMessage;
import messages.ServerMessage;

// un client collegato: legge i suoi messaggi e li smista alla lobby o alla sua stanza
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final Lobby lobby;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private Player player;          // null finche' il client non entra in una stanza
    private RoomController room;    // idem

    public ClientHandler(Socket socket, Lobby lobby) throws IOException {
        this.socket = socket;
        this.lobby = lobby;
        out = new ObjectOutputStream(socket.getOutputStream());   // stesso ordine del client: prima l'output
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
    }

    // non lancia eccezioni: se il client e' sparito si chiude e basta, il server non deve fermarsi
    public synchronized void send(ServerMessage message) {
        try {
            out.writeObject(message);
            out.reset();
            out.flush();
        } catch (IOException e) {
            close();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                ClientMessage message = (ClientMessage) in.readObject();
                try {
                    handle(message);
                } catch (RuntimeException e) {
                    // un errore nel codice del gioco non deve far cadere la connessione
                    send(new ServerMessage.ErrorMessage("Server error: " + e.getMessage()));
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            // il client si e' scollegato (o ha chiuso con Leave)
        } finally {
            close();
        }
    }

    private void handle(ClientMessage message) {
        switch (message) {
            // lobby: sempre permessi
            case ClientMessage.ListRooms() -> send(new ServerMessage.RoomList(lobby.listRooms()));
            case ClientMessage.CreateRoom(String name) -> {
                String id = lobby.createRoom(name);
                send(new ServerMessage.Info("Created room " + id));
                send(new ServerMessage.RoomList(lobby.listRooms()));
            }
            case ClientMessage.JoinRoom(String roomId, String nickname) -> join(roomId, nickname);
            case ClientMessage.CreateAndJoinRoom(String name, String nickname) -> join(lobby.createRoom(name), nickname);
            case ClientMessage.Leave() -> close();

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
            case ClientMessage.PlaceKoi(int type, int q, int r, int angle) -> inRoom(room -> room.placeKoi(player, type, q, r, angle));
            case ClientMessage.MoveKoi(int koiId, int q, int r, int angle) -> inRoom(room -> room.moveKoi(player, koiId, q, r, angle));
           

        }
    }

    private void join(String roomId, String nickname){
        if(room!=null){
            send(new ServerMessage.ErrorMessage("You are already in a room"));
            return;
        }

        RoomController chosen = lobby.getRoom(roomId);
        
        if(chosen==null){
            send(new ServerMessage.ErrorMessage("This room dosn't exist"));
            return;
        }

        Player newPlayer = new Player(nickname);
        if (chosen.addPlayer(newPlayer, this)) {   // Joined lo manda la stanza
            player = newPlayer;
            room = chosen;
        }
    }

    private void inRoom(Consumer<RoomController> action){
        if(room==null){
            send(new ServerMessage.ErrorMessage("Join a room first"));
        } else {
            action.accept(room);
        }
    }

    private void close(){
        try {
            socket.close();   // sblocca anche readObject in run(), che esce con un'eccezione
        } catch (IOException e) {
            // stiamo chiudendo comunque
        }
    }
}

