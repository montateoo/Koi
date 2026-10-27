package client.gui;

import java.util.List;
import java.util.function.IntConsumer;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

// il tabellone: le 5 carte scoperte con, sotto ciascuna, il costo o la ricompensa di quello spazio
public class BoardPane extends HBox {

    // stessi effetti di Board.drawEffect (posizioni 0-4)
    private static final String[] SLOT_EFFECT = {
        "paga 1 moneta",
        "gratis",
        "+2 monete  +1 acqua",
        "1 moneta + 2 acqua\no mercato carpe",
        "2 sponde\no 2 fogliami"
    };

    private static final double CARD_W = 128;
    private static final double CARD_H = 168;

    private IntConsumer onCardClicked = position -> { };
    private List<String> previous = List.of();

    public BoardPane() {
        setSpacing(14);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(6, 0, 4, 0));
    }

    public void setOnCardClicked(IntConsumer handler) {
        this.onCardClicked = handler;
    }

    public void update(List<String> board, boolean canDraw) {
        getChildren().clear();
        for (int i = 0; i < board.size(); i++) {
            VBox slot = new VBox(6, card(board.get(i), i, canDraw), slotLabel(i));
            slot.setAlignment(Pos.TOP_CENTER);
            getChildren().add(slot);
            boolean changed = !previous.isEmpty() && (i >= previous.size() || !previous.get(i).equals(board.get(i)));
            if (changed) {
                slideIn(slot, i);
            }
        }
        previous = board;
    }

    private Node card(String description, int position, boolean canDraw) {
        if (description.isEmpty()) {
            return emptySlot();   // mazzo finito: lo spazio resta vuoto
        }
        String type = description.contains(":") ? description.substring(0, description.indexOf(':')).trim() : description;
        String body = description.contains(":") ? description.substring(description.indexOf(':') + 1).trim() : "";

        Rectangle paper = new Rectangle(CARD_W, CARD_H);
        paper.setArcWidth(14);
        paper.setArcHeight(14);
        paper.setFill(Theme.PAPER_LIGHT);
        paper.setStroke(Theme.INK);
        paper.setStrokeWidth(1.5);

        Rectangle band = new Rectangle(CARD_W - 12, 44, colorOf(type).deriveColor(0, 0.8, 1.15, 0.35));
        band.setArcWidth(10);
        band.setArcHeight(10);

        Text name = new Text(type.toUpperCase());
        name.setFont(Theme.title(type.length() > 7 ? 10.5 : 13));   // "PERGAMENA" e "MAGAZZINO" altrimenti escono dalla carta
        name.setFill(Theme.INK);

        HBox header = new HBox(8, KoiArt.seal(kanjiOf(type), type.substring(0, 1), 30, colorOf(type)), name);
        header.setAlignment(Pos.CENTER_LEFT);
        StackPane headerBox = new StackPane(band, header);
        StackPane.setMargin(header, new Insets(0, 0, 0, 8));

        Text text = new Text(type.equals("Pioggia") ? rainTiles(body) : prettify(body));
        text.setFont(Theme.serif(13));
        text.setFill(Theme.INK);
        text.setWrappingWidth(CARD_W - 20);
        text.setTextAlignment(TextAlignment.CENTER);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Text number = new Text(String.valueOf(position));
        number.setFont(Theme.title(11));
        number.setFill(Theme.INK_SOFT);

        VBox content = new VBox(10, headerBox, text, spacer, number);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(6, 6, 8, 6));
        content.setMaxSize(CARD_W, CARD_H);

        StackPane card = new StackPane(paper, content);
        card.setMaxSize(CARD_W, CARD_H);
        DropShadow shadow = new DropShadow(10, 0, 3, Color.color(0.25, 0.15, 0.05, 0.35));
        card.setEffect(shadow);

        if (canDraw) {
            card.setStyle("-fx-cursor: hand;");
            card.setOnMouseEntered(e -> lift(card, shadow, true));
            card.setOnMouseExited(e -> lift(card, shadow, false));
            card.setOnMouseClicked(e -> onCardClicked.accept(position));
        } else {
            card.setOpacity(0.88);
        }
        return card;
    }

    // spazio del tabellone senza carta (mazzo finito): contorno tratteggiato, non cliccabile
    private static Node emptySlot() {
        Rectangle outline = new Rectangle(CARD_W, CARD_H);
        outline.setArcWidth(14);
        outline.setArcHeight(14);
        outline.setFill(Color.color(1, 1, 1, 0.18));
        outline.setStroke(Theme.INK_SOFT);
        outline.setStrokeWidth(1.5);
        outline.getStrokeDashArray().addAll(8.0, 6.0);
        Text empty = new Text("vuoto");
        empty.setFont(Theme.serif(12));
        empty.setFill(Theme.INK_SOFT);
        StackPane slot = new StackPane(outline, empty);
        slot.setMaxSize(CARD_W, CARD_H);
        return slot;
    }

    private Node slotLabel(int position) {
        Text effect = new Text(position < SLOT_EFFECT.length ? SLOT_EFFECT[position] : "");
        effect.setFont(Theme.serif(11.5));
        effect.setFill(position == 0 ? Theme.VERMILION : Theme.TEAL_DEEP);
        effect.setTextAlignment(TextAlignment.CENTER);
        return effect;
    }

    private void lift(Node card, DropShadow shadow, boolean up) {
        TranslateTransition move = new TranslateTransition(Duration.millis(160), card);
        move.setToY(up ? -8 : 0);
        move.play();
        shadow.setRadius(up ? 18 : 10);
        shadow.setOffsetY(up ? 8 : 3);
    }

    private void slideIn(Node slot, int index) {
        slot.setOpacity(0);
        slot.setTranslateX(-40);
        TranslateTransition move = new TranslateTransition(Duration.millis(420), slot);
        move.setToX(0);
        move.setInterpolator(Interpolator.EASE_OUT);
        FadeTransition fade = new FadeTransition(Duration.millis(420), slot);
        fade.setToValue(1);
        ParallelTransition both = new ParallelTransition(move, fade);
        both.setDelay(Duration.millis(index * 60));
        both.play();
    }

    // le descrizioni arrivano gia' in italiano dal server: qui si toglie solo la parte degli usi
    // ("piazza una carpa o sposta una carpa | usi: 1/1") e si va a capo prima di ogni alternativa
    static String prettify(String body) {
        return body.replaceAll("\\s*\\|\\s*usi:.*", "").replace(" o ", "\no ");
    }

    // "[acqua, acqua, fogliame]" -> "2 acqua + 1 fogliame, piazzate subito"
    private static String rainTiles(String body) {
        java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        for (String tile : body.replace("[", "").replace("]", "").split(",")) {
            if (!tile.isBlank()) {
                counts.merge(tile.trim().toLowerCase(), 1, Integer::sum);
            }
        }
        StringBuilder text = new StringBuilder();
        for (java.util.Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (text.length() > 0) {
                text.append(" + ");
            }
            text.append(entry.getValue()).append(' ').append(entry.getKey());
        }
        return text + "\npiazzate subito";
    }

    static String kanjiOf(String type) {
        switch (type) {
            case "Aiutante":  return "助";
            case "Magazzino": return "蔵";
            case "Mercato":   return "市";
            case "Pioggia":   return "雨";
            case "Pozzo":     return "井";
            case "Pergamena": return "巻";
            default:          return "札";
        }
    }

    static Color colorOf(String type) {
        switch (type) {
            case "Aiutante":  return Theme.TEAL_DEEP;
            case "Magazzino": return Theme.WOOD;
            case "Mercato":   return Theme.GOLD;
            case "Pioggia":   return Color.web("#4a78b5");
            case "Pozzo":     return Theme.STONE_DARK;
            case "Pergamena": return Theme.VERMILION;
            default:          return Theme.INK;
        }
    }
}
