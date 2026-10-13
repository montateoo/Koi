package game.cards;

public class WellCard extends Card {
    private int carriedMoney;
    private int carriedWater;
    private boolean both; //if false only one of two can be choosen

    public WellCard(int id, CardType type, int playerNum, int carriedMoney, int carriedWater, boolean both){
        super (id, type, playerNum);
        this.carriedWater=carriedWater;
        this.carriedMoney=carriedMoney;
        this.both=both;
    }

    public void activate(CardEffects target){
        if(both){
            target.giveWellMoney(carriedMoney);
            target.giveWellWater(carriedWater);
        } else {
            target.giveWellChoice(carriedMoney,carriedWater);
        }
    }
}
