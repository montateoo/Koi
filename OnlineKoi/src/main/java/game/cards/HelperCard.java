package game.cards;

import java.util.ArrayList;
import java.util.List;

import game.Worker;

public class HelperCard extends Card implements Worker {
    private ArrayList<Actions> allowedActions;
    private final int capacity;
    private int usesLeft;

    public HelperCard(int id, CardType type, int playerNum, ArrayList<Actions> allowedActions, int capacity){
        super (id, type, playerNum);
        this.allowedActions = allowedActions;
        this.capacity = capacity;
        this.usesLeft = capacity;
    }

    @Override
    public boolean canDo(Actions requiredAction) {
        return usesLeft > 0 && allowedActions.contains(requiredAction);
    }

    @Override
    public boolean isBusy() {
        return usesLeft > 0 && usesLeft < capacity;
    }

    @Override
    public void work(Actions action) {
        usesLeft--;
    }

    @Override
    public void rest() {
        usesLeft = capacity;
    }

    @Override
    public String describe() {
        List<String> actions = new ArrayList<>();
        for (Actions action : allowedActions) {
            actions.add(describe(action));
        }
        // " | usi: " separa l'azione dagli usi rimasti: lo legge la plancia del client
        return "Aiutante: " + String.join(" o ", actions) + " | usi: " + usesLeft + "/" + capacity;
    }

    private static String describe(Actions action) {
        switch (action) {
            case PLACEWATER:   return "piazza fino a 2 acque";
            case PLACELEAFS:   return "piazza un fogliame";
            case PLACELILY:    return "aggiungi una ninfea";
            case PLACEBORDER:  return "piazza una sponda";
            case PLACELANTERN: return "aggiungi una lanterna";
            case PLACEKOI:     return "piazza una carpa";
            case MOVEKOI:      return "sposta una carpa";
            default:           return action.name().toLowerCase();
        }
    }

}