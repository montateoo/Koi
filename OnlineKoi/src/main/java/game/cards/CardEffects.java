package game.cards;

import java.util.ArrayList;
import game.Tile;

public interface CardEffects {
    void goToMarketWithDiscount(int amount); //MarketCards
    void addStorage(int space);
    void placeRainTiles(ArrayList<Tile> rainTiles); //raintiles
    void giveWellMoney(int money);
    void giveWellWater(int water);
    void giveWellChoice(int money, int water);
}

