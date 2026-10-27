package network;

import java.util.List;

// una stanza nella lista della lobby: id per entrare, nome scelto da chi l'ha creata, chi c'e' gia'.
// open vale false quando la partita e' gia' cominciata (si vede, ma non si puo' entrare)
public record RoomInfo(String id, String name, List<String> players, boolean open) {

    public RoomInfo {
        players = List.copyOf(players);
    }
}
