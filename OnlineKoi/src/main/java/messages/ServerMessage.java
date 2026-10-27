package messages;

import java.io.Serializable;
import java.util.List;

import game.scoring.ScoreSheet;
import network.GameView;
import network.MatchRecord;

// tutto quello che il server manda al client: aggiornamenti, messaggi e le risposte
// che con RMI erano i valori di ritorno (lista delle stanze, ingresso riuscito) o le eccezioni
public sealed interface ServerMessage extends Serializable {

    record GameUpdate(GameView view) implements ServerMessage { }

    record Info(String text) implements ServerMessage { }

    record ErrorMessage(String text) implements ServerMessage { }

    record RoomList(List<String> rooms) implements ServerMessage {
        public RoomList {
            rooms = List.copyOf(rooms);   // copia immutabile: non cambia mentre viaggia
        }
    }

    record Joined(String roomId) implements ServerMessage { }

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
