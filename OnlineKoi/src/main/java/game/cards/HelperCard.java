package game.cards;

import java.util.ArrayList;

public class HelperCard extends Card {
    private ArrayList<Actions> allowedActions;
    private boolean tired; //true if has already worked or false if not

    

    public HelperCard(int id, CardType type, int playerNum, ArrayList<Actions> allowedActions){
        super (id, type, playerNum);
        this.allowedActions=allowedActions;
        this.tired=false;
    }

    public boolean canWork()
    {
        return !tired;
    }

    public void hasWorked()
    {
        tired = true;
    }

    public void returnOperational()
    {
        tired = false;
    }
}