package client;

import java.io.IOException;

import messages.ClientMessage;

public class ClientController {

    private final View view;
    private ServerConnection server;

    public ClientController(View view) {
        this.view = view;
    }

    // unico punto da cui partono i messaggi: controlla la connessione e gestisce l'errore di scrittura
    private void send(ClientMessage message) {
        if (server == null) {
            view.showError("Not connected to the server: check the address in the settings");
            return;
        }
        try {
            server.send(message);
        } catch (IOException e) {
            view.showError("Could not reach the server: " + e.getMessage());
        }
    }


    public boolean connect(String host, int port) {
        if (server != null) {
            server.close();   // cambio di server dalle impostazioni: la vecchia connessione va chiusa
            server = null;
        }
        try {
            server = new ServerConnection(host, port, view);
            return true;
        } catch (IOException e) {
            view.showError("Cannot reach the server " + host + ":" + port + " (" + e.getMessage() + ")");
            return false;
        }
    }

    public void refreshRooms() {
        send(new ClientMessage.ListRooms());
    }

    public void createRoom(String roomName) {
        send(new ClientMessage.CreateRoom(roomName));
    }

    // crea la stanza e ci entra subito (pulsante "Crea Stanza" della schermata iniziale)
    public void createAndJoin(String roomName, String nickname) {
        send(new ClientMessage.CreateAndJoinRoom(roomName, nickname));
    }

    public void joinRoom(String roomId, String nickname) {
        send(new ClientMessage.JoinRoom(roomId, nickname));
    }

    public void startGame() {
        send(new ClientMessage.StartGame());
    }

    public void drawCard(int position) {
        send(new ClientMessage.DrawCard(position));
    }

    public void placeWater(int q, int r){
        send(new ClientMessage.PlaceWater(q,r));
    }

    public void placeLeaf(int q, int r){
        send(new ClientMessage.PlaceLeafs(q,r));
    }

    public void placeLily(int q, int r){
        send(new ClientMessage.PlaceLily(q,r));
    }

    public void placeBorder(int q, int r, int angle) {
        send(new ClientMessage.PlaceBorder(q,r,angle));
    }
    
    public void placeLantern(int q, int r) {
        send(new ClientMessage.PlaceLantern(q,r));
    }

    public void placeKoi(int type, int q, int r, int angle) {
        send(new ClientMessage.PlaceKoi(type,q,r,angle));
    }

    public void moveKoi(int koiId, int q, int r, int angle) {
        send(new ClientMessage.MoveKoi(koiId,q,r,angle));
    }

    public void endTurn() {
        send(new ClientMessage.EndTurn());
    }

    public void choose(int option) {
        send(new ClientMessage.Choose(option));
    }

    public void quit() {
        if (server != null) {
            send(new ClientMessage.Leave());   // avvisa il server (send fa gia' flush: il messaggio parte subito)
            server.close();
        }
        System.exit(0);
    }


}
