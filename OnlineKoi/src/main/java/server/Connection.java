package server;

import messages.ServerMessage;

// il canale verso un client, qualunque sia il trasporto (oggi un WebSocket, vedi WebServer).
// ClientHandler e RoomController parlano solo con questa interfaccia
public interface Connection {

    // non lancia eccezioni: se il client e' sparito il messaggio si perde e il trasporto chiude la connessione
    void send(ServerMessage message);
}
