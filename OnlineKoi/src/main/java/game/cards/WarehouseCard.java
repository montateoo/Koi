package game.cards;

public class WarehouseCard extends Card {
    private int additionalSpace;

    public WarehouseCard(int id, CardType type, int playerNum, int additionalSpace){
        super (id, type, playerNum);
        this.additionalSpace=additionalSpace;
    }

    public void activate(CardEffects target) {
        target.addStorage(additionalSpace);
    }

    @Override
    public String describe() {
        return "Warehouse: +" + additionalSpace + " storage";
    }
}