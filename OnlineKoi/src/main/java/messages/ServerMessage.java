package messages;

import java.util.List;
import java.util.Map;

import game.scoring.ScoreSheet;
import network.GameView;
import network.MatchRecord;
import network.RoomInfo;

// tutto quello che il server manda al client: aggiornamenti, messaggi e le risposte alle richieste.
// Viaggia in JSON con il nome del record nel campo "type" (vedi Json)
public sealed interface ServerMessage {

    // primo messaggio dopo la connessione: i nomi delle tessere da mostrare ("MEDIUMKOI" -> "carpa media"),
    // cosi' il client non ne tiene una copia sua
    record Welcome(Map<String, String> tileNames) implements ServerMessage {
        public Welcome {
            tileNames = Map.copyOf(tileNames);
        }
    }

    record GameUpdate(GameView view) implements ServerMessage { }

    record Info(String text) implements ServerMessage { }

    record ErrorMessage(String text) implements ServerMessage { }

    record RoomList(List<RoomInfo> rooms) implements ServerMessage {
        public RoomList {
            rooms = List.copyOf(rooms);   // copia immutabile: non cambia mentre viene scritta
        }
    }

    record Joined(String roomId, String roomName) implements ServerMessage { }

    // risposta a ListHistory: le partite finite, la piu' recente per prima
    record History(List<MatchRecord> matches) implements ServerMessage {
        public History {
            matches = List.copyOf(matches);
        }
    }

    // fine partita: i fogli dei punti dal primo classificato all'ultimo
    record GameOver(List<ScoreSheet> ranking) implements ServerMessage {
        public GameOver {
            ranking = List.copyOf(ranking);
        }
    }
}
