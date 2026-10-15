package game;

import game.cards.Actions;

public interface Worker {
    boolean canDo(Actions action);   // sa fare l'azione e ha ancora usi
    boolean isBusy();                // ha gia' iniziato a lavorare in questo turno
    void work(Actions action);       // consuma un uso
    void rest();                     // nuovo turno: torna alla capacita' piena
}
