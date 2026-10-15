package game.cards;

import java.util.ArrayList;

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
        return "Helper: " + allowedActions + " usesLeft: " + usesLeft + "/" + capacity;
    }

}