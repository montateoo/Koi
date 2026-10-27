package network;

import java.util.List;

// fotografia di un giocatore inviata ai client: solo testo e numeri, cosi' attraversa la rete senza problemi
public record PlayerView(
        String name,
        int coins,
        int luckyCards,
        int storageUsed,
        int storageMax,
        List<String> tiles,
        List<String> helpers,
        List<String> cards,
        List<String> scrolls,
        List<CellView> pond) {

    public PlayerView {
        tiles = List.copyOf(tiles);
        helpers = List.copyOf(helpers);
        cards = List.copyOf(cards);
        scrolls = List.copyOf(scrolls);
        pond = pond == null ? List.of() : List.copyOf(pond);
    }
}
