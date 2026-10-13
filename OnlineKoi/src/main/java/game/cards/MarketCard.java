package game.cards;

public class MarketCard extends Card {
    private int discount;

    public MarketCard(int id, CardType type, int playerNum, int discount){
        super (id, type, playerNum);
        this.discount=discount;
    }

    public void activate(CardEffects target) {
        target.goToMarketWithDiscount(discount);
    }
    
}
