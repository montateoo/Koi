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
            view.showError("Non sei collegato al server: controlla l'indirizzo nelle impostazioni");
            return;
        }
        try {
            server.send(message);
        } catch (IOException e) {
            view.showError("Il server non risponde: " + e.getMessage());
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
            view.showError("Non riesco a raggiungere il server " + host + ":" + port + " (" + e.getMessage() + ")");
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

    public void spendLuckyCard(){
        send(new ClientMessage.SpendLuckyCard());
    }

    // esce dalla stanza restando collegato (a fine partita: torna al menu)
    public void leaveRoom() {
        send(new ClientMessage.Leave());
    }

    // lo storico arriva a parte, come ServerMessage.History -> view.showHistory
    public void requestHistory() {
        send(new ClientMessage.ListHistory());
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
