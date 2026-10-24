package client;

import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import network.Lobby;
import network.PlayerSession;

// logica del client condivisa da TextView e GuiView: parla con il server e mostra i risultati tramite la view.
// Si catturano anche le RuntimeException perche' un errore nel codice del server arriva al client in quella forma.
public class ClientController {

    private final View view;
    private Lobby lobby;
    private PlayerSession session;
    private ClientCallbackImpl callback;

    public ClientController(View view) {
        this.view = view;
    }

    public boolean connect(String host, int port) {
        try {
            Registry registry = LocateRegistry.getRegistry(host, port);
            lobby = (Lobby) registry.lookup("KoiGame");
            return true;
        } catch (RemoteException | NotBoundException e) {
            view.showError("Cannot reach the server: " + describe(e));
            return false;
        }
    }

    public void refreshRooms() {
        if (!connected()) {
            return;
        }
        try {
            view.showLobby(lobby.listRooms());
        } catch (RemoteException | RuntimeException e) {
            view.showError("Could not load the rooms: " + describe(e));
        }
    }

    public void createRoom(String roomName) {
        if (!connected()) {
            return;
        }
        try {
            String roomId = lobby.createRoom(roomName);
            view.showMessage("Created room " + roomId);
            refreshRooms();
        } catch (RemoteException | RuntimeException e) {
            view.showError("Could not create the room: " + describe(e));
        }
    }

    // crea la stanza e ci entra subito (pulsante "Crea Stanza" della schermata iniziale)
    public void createAndJoin(String roomName, String nickname) {
        if (!connected()) {
            return;
        }
        try {
            joinRoom(lobby.createRoom(roomName), nickname);
        } catch (RemoteException | RuntimeException e) {
            view.showError("Could not create the room: " + describe(e));
        }
    }

    public void joinRoom(String roomId, String nickname) {
        if (!connected()) {
            return;
        }
        if (session != null) {
            view.showError("You are already in a room");
            return;
        }
        try {
            if (callback == null) {
                callback = new ClientCallbackImpl(view);
            }
            session = lobby.join(roomId, nickname, callback);
            view.showRoom(roomId);
        } catch (RemoteException | RuntimeException e) {
            view.showError("Could not join room " + roomId + ": " + describe(e));
        }
    }

    public void startGame() {
        if (session == null) {
            view.showError("Join a room first");
            return;
        }
        try {
            session.startGame();
        } catch (RemoteException | RuntimeException e) {
            view.showError("Could not start the game: " + describe(e));
        }
    }

    public void drawCard(int position) {
        if (session == null) {
            view.showError("Join a room first");
            return;
        }
        try {
            session.drawCard(position);
        } catch (RemoteException | RuntimeException e) {
            view.showError("Could not draw the card: " + describe(e));
        }
    }

    public void placeWater(int q, int r){
        if (session == null){
            view.showError("Join a session first");
            return;
        }
        try {
            session.placeWater(q, r);
        } catch (RemoteException | RuntimeException e) {
            view.showError("Could not place water: " + describe(e));
        }
    }

    public void placeLeaf(int q, int r){
        if (session == null){
            view.showError("Join a session first");
            return;
        }
        try {
            session.placeLeafs(q,r);
        } catch (RemoteException | RuntimeException e) {
            view.showError("Could not place leaf: " + describe(e));
        }
    }

    public void placeLily(int q, int r){
        if (session == null){
            view.showError("Join a session first");
            return;
        }
        try {
            session.placeLily(q,r);
        } catch (RemoteException | RuntimeException e) {
            view.showError("Could not place lily: " + describe(e));
        }
    }

    public void placeBorder(int q, int r, int angle) {
        if (session == null){
            view.showError("Join a session first");
            return;
        }  
        try {
            session.placeBorder(q,r,angle);
        } catch (RemoteException | RuntimeException e) {
            view.showError("Could not place border: " + describe(e));
        } 
    }
    
    public void placeLantern(int q, int r) {
        if (session == null){
            view.showError("Join a session first");
            return;
        }
        try {
            session.placeLantern(q,r);
        } catch (RemoteException | RuntimeException e) {
            view.showError("Could not place lantern: " + describe(e));
        }
    }

    public void placeKoi(int type, int q, int r, int angle) {
        if (session == null){
            view.showError("Join a session first");
            return;
        }
        try {
            session.placeKoi(type,q,r,angle);
        } catch (RemoteException | RuntimeException e) {
            view.showError("Could not place the koi: " + describe(e));
        }
    }

    public void moveKoi(int koiId, int q, int r, int angle) {
        if (session == null){
            view.showError("Join a session first");
            return;
        }
        try {
            session.moveKoi(koiId,q,r,angle);
        } catch (RemoteException | RuntimeException e) {
            view.showError("Could not move the koi: " + describe(e));
        }  
    }

    public void endTurn() {
        try {
            session.endTurn();
        } catch (RemoteException e) {
            view.showError("Could not end the turn: " + describe(e));
        }
    }

    public void choose(int option) {
        if (session == null) {
            view.showError("Join a room first");
            return;
        }
        try {
            session.choose(option);
        } catch (RemoteException | RuntimeException e) {
            view.showError("Could not send your choice: " + describe(e));
        }
    }


    public void quit() {
        try {
            if (session != null) {
                session.leave();
            }
        } catch (Exception e) {
            view.showError("Could not notify the server: " + describe(e));
        } finally {
            if (callback != null) {
                try {
                    UnicastRemoteObject.unexportObject(callback, true);
                } catch (NoSuchObjectException e) {
                    // gia' rimosso, niente da fare
                }
            }
            System.exit(0);
        }
    }

    // senza server raggiungibile le azioni della lobby non possono funzionare: lo si dice chiaramente
    private boolean connected() {
        if (lobby == null) {
            view.showError("Not connected to the server: check the address in the settings");
            return false;
        }
        return true;
    }

    private static String describe(Exception e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }
}
