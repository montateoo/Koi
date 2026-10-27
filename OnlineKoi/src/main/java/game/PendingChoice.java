package game;

import java.util.List;
import java.util.function.IntConsumer;

// una domanda a un giocatore. Se le risposte sono piazzamenti nello stagno, placements ha un elemento per opzione
// (stesso ordine di options): il client mostra le posizioni e l'anteprima, e il clic sceglie l'opzione
public record PendingChoice(Player player, String question, List<String> options, IntConsumer onAnswer,
        List<Placement> placements) {

    public PendingChoice {
        placements = List.copyOf(placements);
    }

    public PendingChoice(Player player, String question, List<String> options, IntConsumer onAnswer) {
        this(player, question, options, onAnswer, List.of());
    }
}
