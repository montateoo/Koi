package network;

// un obiettivo sul tavolo, come lo vede un giocatore preciso.
// type e' il nome dell'ObjectiveType, tileType la tessera da disegnare (per i rami quella che prenderebbe chi guarda);
// claimable vale true se chi guarda puo' reclamarlo a fine turno (il Percorso si reclama piazzando una carpa)
public record ObjectiveView(
        String type,
        String name,
        String requirement,
        int cost,
        int tilesLeft,
        String tileType,
        boolean claimable) {
}
