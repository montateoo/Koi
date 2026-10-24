package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {

    public static final int PORT = 1099;   // la stessa che usano TextView e le Impostazioni del client

    public static void main(String[] args) throws IOException {
        Lobby lobby = new Lobby();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server ready on port " + PORT);
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
                e1.printStackTrace();
            }
        }
    }
}

