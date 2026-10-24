package client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import network.CellView;
import network.GameView;
import network.HexCoord;
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
        out.append("Turn: ").append(view.activePlayer())
           .append(view.isMyTurn() ? "   <<< your turn" : "").append('\n');

        out.append('\n').append("Board").append(view.isMyTurn() ? "  (draw <position>)" : "").append('\n');
        for (int i = 0; i < view.board().size(); i++) {
            out.append(String.format("  [%d] %s%n", i, view.board().get(i)));
        }

        out.append(String.format("%nKoi market: small %d | medium %d | big %d%n",
                view.smallKoiLeft(), view.mediumKoiLeft(), view.bigKoiLeft()));

        for (PlayerView player : view.players()) {
            String name = player.name() + (player.name().equals(view.viewer()) ? " (you)" : "");
            out.append("-".repeat(WIDTH)).append('\n');
            out.append(String.format("%-22s coins %-3d lucky %-3d storage %d/%d%n",
                    name, player.coins(), player.luckyCards(), player.storageUsed(), player.storageMax()));
            out.append("  tiles:   ").append(countSame(player.tiles())).append('\n');
            out.append("  helpers: ").append(listOrDash(player.helpers())).append('\n');
            out.append("  cards:   ").append(listOrDash(player.cards())).append('\n');
            out.append("  scrolls: ").append(listOrDash(player.scrolls())).append('\n');
        }

        if (!view.objectives().isEmpty()) {
            out.append('\n').append("Objectives").append('\n');
            for (ObjectiveView objective : view.objectives()) {
                out.append(String.format("  %-15s %s%s  [%d left]%s%n", objective.name(),
                        objective.cost() > 0 ? objective.cost() + " coins, " : "", objective.requirement(),
                        objective.tilesLeft(), objective.claimable() ? "  <<< claimable (end)" : ""));
            }
        }

        if (!view.playablePositions().isEmpty()) {
            out.append('\n').append("You can place:").append('\n');
            for (Map.Entry<String, List<HexCoord>> entry : view.playablePositions().entrySet()) {
                String type = entry.getKey();
                StringBuilder spots = new StringBuilder();
                for (HexCoord anchor : entry.getValue()) {
                    spots.append(anchor);
                    // sponda e carpe grandi si piazzano anche con una rotazione: mostro quelle valide
                    List<Integer> rotations = view.rotationsAt(type, anchor);
                    if (!rotations.isEmpty()) {
                        spots.append(" rot ").append(rotations);
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
                out.append("   (choose <number>)\n");
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
            return "move " + koiName(koiId, view) + " (move " + koiId + ")";
        }
        switch (type) {
            case "SMALLKOI":  return "small koi (koi 0)";
            case "MEDIUMKOI": return "medium koi (koi 1)";
            case "BIGKOI":    return "big koi (koi 2)";
            default:          return type.toLowerCase();
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
                    return cell.top().replace("KOI", " koi").toLowerCase();
                }
            }
        }
        return "koi";
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
            text.append(entry.getKey()).append(" x").append(entry.getValue());
        }
        return text.toString();
    }

    private static String listOrDash(List<String> items) {
        return items.isEmpty() ? "-" : String.join(" | ", items);
    }
}
