package client.gui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Text;
import game.TileType;
import network.PlayerView;

// la "plancia Sensei" del giocatore: monete, fortuna, magazzino a esagoni di legno, aiutanti e pergamene.
// Cliccando una tessera del magazzino la si seleziona per piazzarla nello stagno.
public class PlayerPanel extends VBox {

    private Consumer<String> onTileSelected = type -> { };
    private Runnable onSpendLucky = () -> { };

    public PlayerPanel() {
        setSpacing(12);
        getStyleClass().add("panel");
        setPrefWidth(270);
        setMinWidth(270);
    }

    // clic sui segnalini Fortuna: chiede al server di spenderne uno (le opzioni arrivano come domanda)
    public void setOnSpendLucky(Runnable handler) {
        this.onSpendLucky = handler;
    }

    public void setOnTileSelected(Consumer<String> handler) {
        this.onTileSelected = handler;
    }

    // placeable: i tipi che ora si possono piazzare (dalle posizioni valide della view); selected: quello scelto
    public void update(PlayerView me, Set<String> placeable, String selected) {
        getChildren().clear();

        Text name = new Text(me.name());
        name.setFont(Theme.title(22));
        name.setFill(Theme.INK);

        HBox wealth = new HBox(16,
                withCount(KoiArt.coin(11), me.coins(), "monete"),
                fortune(me.luckyCards()));
        wealth.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(name, wealth, sectionTitle("Magazzino  " + me.storageUsed() + "/" + me.storageMax()),
                storageSlots(me.storageUsed(), me.storageMax()), tiles(me.tiles(), placeable, selected));

        if (!me.helpers().isEmpty()) {
            getChildren().add(sectionTitle("Aiutanti"));
            for (String helper : me.helpers()) {
                // "Aiutante: piazza fino a 2 acque | usi: 1/2" -> "piazza fino a 2 acque   (1/2 in questo turno)"
                String uses = helper.contains("usi:") ? helper.substring(helper.indexOf("usi:") + 4).trim() : "";
                String text = BoardPane.prettify(helper.replace("Aiutante:", "").trim()).replace("\n", " ");
                getChildren().add(cardLine("助", text + (uses.isEmpty() ? "" : "   (" + uses + " in questo turno)"), Theme.TEAL_DEEP));
            }
        }
        if (!me.scrolls().isEmpty()) {
            getChildren().add(sectionTitle("Pergamene"));
            for (String scroll : me.scrolls()) {
                getChildren().add(cardLine("巻", scroll.replace("Pergamena:", "").trim(), Theme.VERMILION));
            }
        }
        if (!me.cards().isEmpty()) {
            getChildren().add(sectionTitle("Carte"));
            for (String card : me.cards()) {
                getChildren().add(cardLine("札", card, Theme.INK_SOFT));
            }
        }
    }

    private Node sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("panel-title");
        return label;
    }

    // segnalini Fortuna: cliccabili quando ce n'e' almeno uno
    private Node fortune(int count) {
        Node box = withCount(KoiArt.fortune(11), count, "segnalini Fortuna");
        if (count > 0) {
            box.setCursor(Cursor.HAND);
            box.setOnMouseClicked(e -> onSpendLucky.run());
            Tooltip.install(box, new Tooltip("Clicca per spendere un segnalino Fortuna: 1 fogliame, 2 acqua, 1 sponda o una carpa a +1 moneta"));
        }
        return box;
    }

    private Node withCount(Node icon, int count, String tooltip) {
        Text number = new Text("× " + count);
        number.setFont(Theme.title(16));
        number.setFill(Theme.INK);
        HBox box = new HBox(6, icon, number);
        box.setAlignment(Pos.CENTER_LEFT);
        Tooltip.install(box, new Tooltip(count + " " + tooltip));
        return box;
    }

    // il magazzino come sulla plancia: esagoni di legno, scuri quelli occupati
    private Node storageSlots(int used, int max) {
        HBox slots = new HBox(3);
        // oltre la capienza (possibile durante il turno) gli esagoni in piu' sono vermigli: a fine turno si scarta
        for (int i = 0; i < Math.max(used, max); i++) {
            Polygon slot = KoiArt.hexagon(0, 0, 11);
            slot.setFill(i >= max ? Theme.VERMILION : i < used ? Theme.WOOD_DARK : Theme.WOOD.deriveColor(0, 0.8, 1.25, 0.6));
            slot.setStroke(Theme.WOOD_DARK);
            slots.getChildren().add(slot);
        }
        return slots;
    }

    // una pedina per tipo di tessera, con il numero; le ninfee e le lanterne vengono dalla riserva
    private Node tiles(List<String> storage, Set<String> placeable, String selected) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String tile : storage) {
            counts.merge(tile, 1, Integer::sum);
        }
        for (String upgrade : List.of("LILY", "LANTERN")) {
            if (placeable.contains(upgrade)) {
                counts.putIfAbsent(upgrade, 0);   // 0 = dalla riserva
            }
        }

        FlowPane tokens = new FlowPane(10, 10);
        if (counts.isEmpty()) {
            Label empty = new Label("vuoto");
            empty.getStyleClass().add("muted");
            tokens.getChildren().add(empty);
        }
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            tokens.getChildren().add(token(entry.getKey(), entry.getValue(), placeable.contains(entry.getKey()),
                    entry.getKey().equals(selected)));
        }
        return tokens;
    }

    private Node token(String type, int count, boolean placeable, boolean selected) {
        Group icon = new Group(KoiArt.tileIcon(type, 21));
        Text badge = new Text(count == 0 ? "riserva" : "× " + count);
        badge.setFont(Theme.serif(11));
        badge.setFill(Theme.INK);
        VBox token = new VBox(2, icon, badge);
        token.setAlignment(Pos.CENTER);
        token.setPadding(new Insets(4));
        Tooltip.install(token, new Tooltip(readable(type) + (placeable ? "  (clicca, poi clicca un esagono evidenziato)" : "")));

        if (selected) {
            token.setStyle("-fx-background-color: rgba(212,161,58,0.35); -fx-background-radius: 8;");
            icon.setEffect(new DropShadow(14, Theme.GOLD));
        }
        if (placeable) {
            token.setStyle(token.getStyle() + "-fx-cursor: hand;");
            token.setOnMouseClicked(e -> onTileSelected.accept(type));
            token.setOnMouseEntered(e -> {
                icon.setScaleX(1.12);
                icon.setScaleY(1.12);
            });
            token.setOnMouseExited(e -> {
                icon.setScaleX(1);
                icon.setScaleY(1);
            });
        } else {
            token.setOpacity(0.6);
        }
        return token;
    }

    private Node cardLine(String kanji, String text, Color color) {
        Label label = new Label(BoardPane.prettify(text));
        label.setWrapText(true);
        label.setMaxWidth(200);
        HBox line = new HBox(8, KoiArt.seal(kanji, kanji.equals("助") ? "H" : kanji.equals("巻") ? "S" : "C", 22, color), label);
        line.setAlignment(Pos.CENTER_LEFT);
        return line;
    }

    // nome da mostrare di un tipo di tessera ("MEDIUMKOI" -> "Carpa media"): viene da TileType, un solo posto
    static String readable(String type) {
        try {
            String name = TileType.valueOf(type).displayName();
            return Character.toUpperCase(name.charAt(0)) + name.substring(1);
        } catch (IllegalArgumentException e) {
            return type.toLowerCase();
        }
    }

    // usato per gli avversari: riga compatta con le stesse icone
    static Node summary(PlayerView player) {
        Text name = new Text(player.name());
        name.setFont(Theme.title(15));
        name.setFill(Theme.INK);
        HBox stats = new HBox(10,
                small(KoiArt.coin(7), player.coins()),
                small(KoiArt.fortune(7), player.luckyCards()),
                new Label("magazzino " + player.storageUsed() + "/" + player.storageMax()));
        stats.setAlignment(Pos.CENTER_LEFT);
        return new VBox(2, name, stats);
    }

    private static Node small(Node icon, int count) {
        Label number = new Label("× " + count);
        HBox box = new HBox(4, new StackPane(icon), number);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }
}
