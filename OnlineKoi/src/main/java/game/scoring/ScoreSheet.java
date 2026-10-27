package game.scoring;

import java.io.Serializable;

// il foglio del blocchetto segnapunti di un giocatore: una voce per riga, piu' le monete per lo spareggio.
// Serializable: viaggia ai client dentro ServerMessage.GameOver (e' nel filtro FROM_SERVER)
public record ScoreSheet(String name, int koi, int lanterns, int scrolls, int objectives, int fortune, int coins)
        implements Serializable {

    // le monete non sono punti: servono solo a decidere i pareggi
    public int total() {
        return koi + lanterns + scrolls + objectives + fortune;
    }
}
