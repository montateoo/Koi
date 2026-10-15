package game.cards;

import java.util.ArrayList;

import game.TileType;
import game.Tile;

public class RainCard extends Card {
    private ArrayList<Tile> rainTiles;

    public RainCard(int id, CardType type, int playerNum, ArrayList<TileType> rainTiles){
        super (id, type, playerNum);

        this.rainTiles = new ArrayList<>();

        for(int i = 0; i<rainTiles.size(); i++){
            this.rainTiles.add(new Tile(rainTiles.get(i)));
        }
        
    }

    public void activate(CardEffects target) {
        target.placeRainTiles(rainTiles);
    }

    @Override
    public String describe() {
        ArrayList<TileType> types = new ArrayList<>();
        for (Tile tile : rainTiles) {
            types.add(tile.getTileType());
        }
        return "Rain: " + types;
    }
    
}