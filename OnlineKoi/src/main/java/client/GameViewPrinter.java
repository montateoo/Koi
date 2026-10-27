package client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import game.TileType;
import game.scoring.ScoreSheet;
import network.CellView;
import network.GameView;
import network.HexCoord;
import network.MatchRecord;
import network.ObjectiveView;
import network.PlayerView;

// trasforma una GameView in testo: usato dalla TUI e, per ora, anche dal log della GUI
public final class GameViewPrinter {

    private static final int WIDTH = 64;
  
    private GameViewPrinter() {
    }

    public static String render(GameView view) {
        StringBuilder out = new StringBuilder();
        out.append("=".repeat(WIDTH)).append('\n');
        out.append("Turno di: ").append(view.activePlayer())
           .append(view.isMyTurn() ? "   <<< tocca a te" : "").append('\n');

        out.append('\n').append("Tabellone").append(view.isMyTurn() ? "  (draw <posizione>)" : "").append('\n');
        for (int i = 0; i < view.board().size(); i++) {
            String card = view.board().get(i);
            out.append(String.format("  [%d] %s%n", i, card.isEmpty() ? "(vuoto)" : card));
        }

        out.append(String.format("%nMercato carpe: piccole %d | medie %d | grandi %d%n",
                view.smallKoiLeft(), view.mediumKoiLeft(), view.bigKoiLeft()));

        for (PlayerView player : view.players()) {
            String name = player.name() + (player.name().equals(view.viewer()) ? " (tu)" : "");
            out.append("-".repeat(WIDTH)).append('\n');
            out.append(String.format("%-22s monete %-3d fortuna %-3d magazzino %d/%d%n",
                    name, player.coins(), player.luckyCards(), player.storageUsed(), player.storageMax()));
            out.append("  tessere:   ").append(countSame(player.tiles())).append('\n');
            out.append("  aiutanti:  ").append(listOrDash(player.helpers())).append('\n');
            out.append("  carte:     ").append(listOrDash(player.cards())).append('\n');
            out.append("  pergamene: ").append(listOrDash(player.scrolls())).append('\n');
        }

        if (!view.objectives().isEmpty()) {
            out.append('\n').append("Obiettivi").append('\n');
            for (ObjectiveView objective : view.objectives()) {
                out.append(String.format("  %-17s %s%s  [%d %s]%s%n", objective.name(),
                        objective.cost() > 0 ? objective.cost() + " monete, " : "", objective.requirement(),
                        objective.tilesLeft(), objective.tilesLeft() == 1 ? "rimasta" : "rimaste", objective.claimable() ? "  <<< reclamabile (end)" : ""));
            }
        }

        if (!view.playablePositions().isEmpty()) {
            out.append('\n').append("Puoi piazzare:").append('\n');
            for (Map.Entry<String, List<HexCoord>> entry : view.playablePositions().entrySet()) {
                String type = entry.getKey();
                StringBuilder spots = new StringBuilder();
                for (HexCoord anchor : entry.getValue()) {
                    spots.append(anchor);
                    // sponda e carpe grandi si piazzano anche con una rotazione: mostro quelle valide
                    List<Integer> rotations = view.rotationsAt(type, anchor);
                    if (!rotations.isEmpty()) {
                        spots.append(" rotazioni ").append(rotations);
                    }
                    spots.append("  ");
                }
                out.append(String.format("  %-19s %s%n", label(type, view), spots.toString().trim()));
            }
        }

        if (view.question() != null) {
            out.append('\n').append(">> ").append(view.question()).append('\n');
            for (int i = 0; i < view.options().size(); i++) {
                out.append(String.format("   [%d] %s%n", i, view.options().get(i)));
            }
            if (!view.options().isEmpty()) {
                out.append("   (choose <numero>)\n");
            }
        }

        out.append("=".repeat(WIDTH));
        return out.toString();
    }

    // classifica finale come il blocchetto segnapunti: una colonna per voce, il totale, le monete per lo spareggio
    public static String renderScores(List<ScoreSheet> ranking) {
        StringBuilder out = new StringBuilder();
        out.append("=".repeat(WIDTH)).append('\n');
        out.append("PARTITA FINITA - vince ").append(ranking.get(0).name()).append("!").append('\n');
        out.append('\n');
        out.append(String.format("  %-3s %-14s %5s %5s %5s %5s %5s %6s %6s%n",
                "#", "giocatore", "carpe", "lant", "perg", "obiet", "fort", "TOTALE", "monete"));
        for (int i = 0; i < ranking.size(); i++) {
            ScoreSheet s = ranking.get(i);
            out.append(String.format("  %-3s %-14s %5d %5d %5d %5d %5d %6d %6d%n",
                    (i + 1) + ".", s.name(), s.koi(), s.lanterns(), s.scrolls(), s.objectives(), s.fortune(),
                    s.total(), s.coins()));
        }
        out.append('\n').append("Scrivi 'leave' per tornare alle stanze, 'history' per lo storico, 'quit' per uscire.").append('\n');
        out.append("=".repeat(WIDTH));
        return out.toString();
    }

    // storico delle partite finite: per ognuna stanza, data, vincitore e la classifica completa
    public static String renderHistory(List<MatchRecord> matches) {
        StringBuilder out = new StringBuilder();
        out.append("=".repeat(WIDTH)).append('\n');
        out.append("STORICO PARTITE").append('\n');
        if (matches.isEmpty()) {
            out.append("  Nessuna partita finita.").append('\n');
        }
        for (int m = 0; m < matches.size(); m++) {
            MatchRecord match = matches.get(m);
            out.append('\n').append(String.format("[%d] %s  -  %s  -  vince %s (%d punti)%n", m + 1,
                    match.roomName(), match.endedAt(), match.winner().name(), match.winner().total()));
            for (int i = 0; i < match.ranking().size(); i++) {
                ScoreSheet s = match.ranking().get(i);
                out.append(String.format("    %d. %-14s carpe %3d  lant %3d  perg %3d  obiet %3d  fort %2d  = %3d  (monete %d)%n",
                        i + 1, s.name(), s.koi(), s.lanterns(), s.scrolls(), s.objectives(), s.fortune(), s.total(), s.coins()));
            }
        }
        out.append("=".repeat(WIDTH));
        return out.toString();
    }

    // nome leggibile del tipo; per le carpe anche il numero da usare nel comando "koi <tipo> <q> <r> <rotazione>"
    // o, per una carpa gia' nello stagno, l'id da usare in "move <id> <q> <r> <rotazione>"
    private static String label(String type, GameView view) {
        if (GameView.isMoveKey(type)) {
            int koiId = GameView.koiIdOf(type);
            return "sposta " + koiName(koiId, view) + " (move " + koiId + ")";
        }
        switch (type) {
            case "SMALLKOI":  return "carpa piccola (koi 0)";
            case "MEDIUMKOI": return "carpa media (koi 1)";
            case "BIGKOI":    return "carpa grande (koi 2)";
            default:          return nameOf(type);
        }
    }

    // "small koi", "medium koi"... cercando la carpa con quell'id nello stagno di chi guarda
    private static String koiName(int koiId, GameView view) {
        for (PlayerView player : view.players()) {
            if (!player.name().equals(view.viewer())) {
                continue;
            }
            for (CellView cell : player.pond()) {
                if (cell.topId() == koiId) {
                    return TileType.valueOf(cell.top()).displayName();
                }
            }
        }
        return "carpa";
    }

    // [WATER, WATER, SMALLKOI] -> "WATER x2, SMALLKOI x1"
    private static String countSame(List<String> items) {
        if (items.isEmpty()) {
            return "-";
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String item : items) {
            counts.merge(item, 1, Integer::sum);
        }
        StringBuilder text = new StringBuilder();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (text.length() > 0) {
                text.append(", ");
            }
            text.append(nameOf(entry.getKey())).append(" x").append(entry.getValue());
        }
        return text.toString();
    }

    // "MEDIUMKOI" -> "carpa media": i nomi vengono da TileType
    private static String nameOf(String type) {
        try {
            return TileType.valueOf(type).displayName();
        } catch (IllegalArgumentException e) {
            return type.toLowerCase();
        }
    }

    private static String listOrDash(List<String> items) {
        return items.isEmpty() ? "-" : String.join(" | ", items);
    }
}
