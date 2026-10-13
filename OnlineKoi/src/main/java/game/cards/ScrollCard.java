package game.cards;

public class ScrollCard extends Card {
    private int pointsPerObject;
    private ScrollObjective objectiveObject;

    public ScrollCard(int id, CardType type, int playerNum, int pointsPerObject, ScrollObjective objectiveObject){
        super (id, type, playerNum);
        this.pointsPerObject=pointsPerObject;
        this.objectiveObject=objectiveObject;
    }
    
}
