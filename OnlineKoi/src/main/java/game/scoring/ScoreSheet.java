package game.scoring;

// il foglio del blocchetto segnapunti di un giocatore: una voce per riga, piu' le monete per lo spareggio.
// Viaggia ai client dentro ServerMessage.GameOver; in JSON c'e' anche il totale (vedi messages.Json)
public record ScoreSheet(String name, int koi, int lanterns, int scrolls, int objectives, int fortune, int coins) {

    // le monete non sono punti: servono solo a decidere i pareggi
    public int total() {
        return koi + lanterns + scrolls + objectives + fortune;
    }
}
