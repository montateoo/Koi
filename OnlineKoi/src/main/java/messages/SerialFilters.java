package messages;

import java.io.ObjectInputFilter;

// readObject ricostruisce qualunque classe serializzabile gli arrivi: chi controlla l'altro capo della connessione
// potrebbe mandare oggetti costruiti apposta per eseguire codice. Questi filtri accettano solo le classi del
// protocollo e rifiutano tutto il resto ("!*" alla fine); i limiti impediscono oggetti enormi o annidati all'infinito.
public final class SerialFilters {

    // il server riceve solo i messaggi dei client: record con numeri e stringhe, niente collezioni
    public static final ObjectInputFilter FROM_CLIENT = ObjectInputFilter.Config.createFilter(
            "maxdepth=5;maxarray=256;"
            + "messages.ClientMessage$*;java.lang.String;"
            + "!*");

    // il client riceve la GameView: record di network, liste immutabili (viaggiano come CollSer),
    // mappe non modificabili su LinkedHashMap, numeri e booleani dentro le liste
    public static final ObjectInputFilter FROM_SERVER = ObjectInputFilter.Config.createFilter(
            "maxdepth=20;maxarray=100000;"
            + "messages.ServerMessage$*;network.*;game.scoring.ScoreSheet;"
            + "java.lang.String;java.lang.Integer;java.lang.Boolean;java.lang.Number;java.lang.Object;"
            + "java.util.CollSer;java.util.ImmutableCollections$*;java.util.Collections$UnmodifiableMap;"
            + "java.util.LinkedHashMap;java.util.HashMap;"
            + "java.util.Map$Entry;"   // HashMap.readObject chiede il permesso di allocare la sua tabella di Map.Entry
            + "!*");

    private SerialFilters() {
    }
}
