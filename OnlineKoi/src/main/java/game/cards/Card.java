package game.cards;

public class Card {
    private int id;
    private CardType type;
    private int playerNum; //min ammount of players to have this cards in the current game
    //private asset 

    public Card(int id, CardType type, int playerNum){
        this.id=id;
        this.type=type;
        this.playerNum=playerNum;
    }

    public int getId(){
        return id;
    }

    public CardType getType(){
        return type;
    }

    public int getPlayerNum(){
        return playerNum;
    }

    public void activate(CardEffects target) {
        // di base non fa nulla
    }

    // descrizione leggibile mostrata ai giocatori, ogni tipo di carta la ridefinisce
    public String describe() {
        return type.toString();
    }

    public String toString(){
        return "card" + id + " Type: " + type.toString() + " Pnum: " + playerNum;
    }


}