package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

import messages.ClientMessage;
import messages.ServerMessage;
import network.GameView;

public class ServerConnection {
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private final View view;
    private volatile boolean closing; //se true abbiamo disconnesso regolarmente alrimenti errore

    public ServerConnection(String host, int port, View view) throws IOException {
        this.view = view;
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 5000);   // massimo 5 secondi
        out = new ObjectOutputStream(socket.getOutputStream());   // PRIMA l'output...
        out.flush();                                              // ...e flush
        in = new ObjectInputStream(socket.getInputStream());      // POI l'input
        
        Thread.ofVirtual().start(this::listen);
    }

    public synchronized void send(ClientMessage message) throws IOException {
        out.writeObject(message); //serializza il record e lo scrive
        out.reset();    //svuota la cache: il prossimo oggetto viaggia completo
        out.flush();    //manda i dati subito senza aspettare che si riempia il buffer
    }

    private void listen() {
        try {
            while (true) {
                ServerMessage message = (ServerMessage) in.readObject();   // si blocca finche' non arriva qualcosa
                switch (message) {
                    case ServerMessage.GameUpdate(GameView gameView) -> view.showGame(gameView);
                    case ServerMessage.Info(String text) -> view.showMessage(text);
                    case ServerMessage.ErrorMessage(String text) -> view.showError(text);
                    case ServerMessage.RoomList(List<String> rooms) -> view.showLobby(rooms);
                    case ServerMessage.Joined(String roomId) -> view.showRoom(roomId);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            if (!closing) {
                view.showError("Connection to the server lost");
            }
        }
    }

    public void close() {
        closing = true;
        try {
            socket.close();   // sblocca anche readObject in listen(), che esce con un'eccezione
        } catch (IOException e) {
            // stiamo chiudendo comunque
        }
    }

}
