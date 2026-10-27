package messages;

import java.io.Serializable;

// tutto quello che il client puo' chiedere al server: un record per ogni azione.
// "sealed": solo questi record sono ClientMessage, cosi' uno switch del server che ne dimentica uno non compila
public sealed interface ClientMessage extends Serializable {

    // ---------------------------------------------------------------- lobby

    record ListRooms() implements ClientMessage { }

    record CreateRoom(String name) implements ClientMessage { }

    // non serve piu' la callback: la connessione stessa e' il canale verso il client
    record JoinRoom(String roomId, String nickname) implements ClientMessage { }

    record CreateAndJoinRoom(String name, String nickname) implements ClientMessage{}

    // lo storico delle partite finite: la risposta e' ServerMessage.History
    record ListHistory() implements ClientMessage { }

    // ---------------------------------------------------------------- stanza

    record StartGame() implements ClientMessage { }

    // esce dalla stanza ma resta collegato (torna al menu); se la connessione cade il server se ne accorge da solo
    record Leave() implements ClientMessage { }

    record DrawCard(int position) implements ClientMessage { }

    record Choose(int option) implements ClientMessage { }

    record EndTurn() implements ClientMessage { }

    // ---------------------------------------------------------------- piazzamenti

    record PlaceWater(int q, int r) implements ClientMessage { }

    record PlaceLeafs(int q, int r) implements ClientMessage { }

    record PlaceLily(int q, int r) implements ClientMessage { }

    record PlaceLantern(int q, int r) implements ClientMessage { }

    record PlaceBorder(int q, int r, int angle) implements ClientMessage { }

    // type: 0 piccola, 1 media, 2 grande
    record PlaceKoi(int type, int q, int r, int angle) implements ClientMessage { }

    record MoveKoi(int koiId, int q, int r, int angle) implements ClientMessage { }

    record SpendLuckyCard() implements ClientMessage { }
}
