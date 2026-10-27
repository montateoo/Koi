package game.cards;

public class ScrollCard extends Card {
    private int pointsPerObject;
    private ScrollObjective objectiveObject;

    public ScrollCard(int id, CardType type, int playerNum, int pointsPerObject, ScrollObjective objectiveObject){
        super (id, type, playerNum);
        this.pointsPerObject=pointsPerObject;
        this.objectiveObject=objectiveObject;
    }

    @Override
    public String describe() {
        return "Pergamena: " + pointsPerObject + " punti per ogni " + describe(objectiveObject);
    }

    private static String describe(ScrollObjective object) {
        switch (object) {
            case BIGKOI:    return "carpa grande";
            case MEDIUMKOI: return "carpa media";
            case SMALLKOI:  return "carpa piccola";
            case LANTERN:   return "lanterna";
            case LILY:      return "ninfea";
            case WELL:      return "carta Pozzo";
            case MARKET:    return "carta Mercato";
            default:        return "carta Magazzino";
        }
    }

    public ScrollObjective getScrollObjective(){
        return objectiveObject;
    }

    public int getPointsPerObject(){
        return pointsPerObject;
    }
    
}
