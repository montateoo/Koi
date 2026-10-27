package game.objectives;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import game.Tile;
import game.TileType;
import game.Player;

public class Objective {

    private Set<Player> claimedByPlayers;

    private ArrayList<Tile> associatedTiles;
    private ObjectiveType objectiveType;

    public Objective(ObjectiveType objectiveType, ArrayList<Tile> associatedTiles) {
        this.claimedByPlayers = new HashSet<>();
        this.objectiveType = objectiveType;
        this.associatedTiles = associatedTiles;
    }

    // la tessera che il giocatore prenderebbe adesso, null se non puo' prenderne nessuna
    private Tile tileFor(Player player) {
        return objectiveType.pickTile(player, Collections.unmodifiableList(associatedTiles));
    }

    public boolean canBeClaimedBy(Player player) {
        return (objectiveType == ObjectiveType.PATH || !claimedByPlayers.contains(player))
                && player.getInventory().getCoins() >= objectiveType.cost()
                && tileFor(player) != null;
    }

    public Tile claim(Player player) {
        Tile tile = tileFor(player);
        associatedTiles.remove(tile);
        claimedByPlayers.add(player);
        player.pay(objectiveType.cost());
        player.addObjective(tile);   // unico punto in cui il giocatore registra le tessere obiettivo (servono al punteggio)
        return tile;
    }

    // testo dell'opzione: dice anche quale tessera si prende (utile per i rami)
    public String describeFor(Player player) {
        String name = tileFor(player).getTileType().displayName();
        String text = Character.toUpperCase(name.charAt(0)) + name.substring(1);   // "ponticello" -> "Ponticello"
        if (objectiveType.cost() > 0) {
            text += " (" + objectiveType.cost() + " monete)";
        }
        return text;
    }

    public int tilesLeft() {
        return associatedTiles.size();
    }

    // tessera da mostrare: quella che il giocatore prenderebbe adesso (conta per i rami), altrimenti la prossima;
    // null se sono finite
    public TileType previewTileType(Player player) {
        Tile tile = tileFor(player);
        if (tile == null && !associatedTiles.isEmpty()) {
            tile = associatedTiles.get(0);
        }
        return tile == null ? null : tile.getTileType();
    }

    public ObjectiveType getObjectiveType() {
        return objectiveType;
    }
}
