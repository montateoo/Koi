package network;

import java.io.Serializable;
import java.util.List;

import game.scoring.ScoreSheet;

// una partita finita, per lo storico: la stanza, quando e' finita, la classifica (dal primo all'ultimo)
// e la fotografia finale di ogni giocatore, stagno compreso
public record MatchRecord(String roomName, String endedAt, List<ScoreSheet> ranking, List<PlayerView> players)
        implements Serializable {

    public MatchRecord {
        ranking = List.copyOf(ranking);
        players = List.copyOf(players);
    }

    public ScoreSheet winner() {
        return ranking.get(0);
    }
}
