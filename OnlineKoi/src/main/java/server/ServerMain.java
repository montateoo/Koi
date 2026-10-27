package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {

    public static final int PORT = 1099;   // la stessa che usano TextView e le Impostazioni del client

    public static void main(String[] args) throws IOException {
        HistoryStore store = new HistoryStore(HistoryStore.defaultLocation());
        Lobby lobby = new Lobby(store);
        System.out.println("Storico partite: " + store.file() + " (" + lobby.history().size() + (lobby.history().size() == 1 ? " partita)" : " partite)"));
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server pronto sulla porta " + PORT);
            while (true) {
                Socket socket = serverSocket.accept();   // aspetta il prossimo client
                Thread.ofVirtual().start(() -> serve(socket, lobby));
            }
        }
    }

    private static void serve(Socket socket, Lobby lobby) {

        try {
            ClientHandler clientHandler = new ClientHandler(socket,lobby);
            clientHandler.run();
        } catch (IOException e) {
            try {
                socket.close();
            } catch (IOException e1) {
                // il client non ha mai parlato: niente da fare
            }
        }
    }
}

