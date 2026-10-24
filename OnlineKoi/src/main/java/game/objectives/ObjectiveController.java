package game.objectives;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import game.Player;

public class ObjectiveController {
    private List<Objective> objectives;

    public ObjectiveController(){
    }

    public void initializeObjectives(int playerNum) {
        List<ObjectiveType> all = new ArrayList<>(List.of(ObjectiveType.values()));
        Collections.shuffle(all);
        objectives = new ArrayList<>();
        for (ObjectiveType type : all.subList(0, 3)) {
            objectives.add(type.build(playerNum));   // il tuo buildX, uno per tipo
        }
    }

    public List<Objective> checkCompletedObjectives(Player player){
        List<Objective> availableObjectives = new ArrayList<>();

        for(Objective o: objectives){
            // il Percorso non si reclama a fine turno ma piazzando una carpa
            if(o.getObjectiveType() != ObjectiveType.PATH && o.canBeClaimedBy(player)){
                availableObjectives.add(o);
            }
        }

        return availableObjectives;
    }

    // i tre obiettivi sul tavolo (vuoto prima dell'inizio della partita)
    public List<Objective> getObjectives() {
        return objectives == null ? List.of() : Collections.unmodifiableList(objectives);
    }

    // l'obiettivo di quel tipo se e' tra quelli sul tavolo, altrimenti null
    public Objective find(ObjectiveType type) {
        for (Objective o : objectives) {
            if (o.getObjectiveType() == type) {
                return o;
            }
        }
        return null;
    }
}
