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
        "pay 1 coin",
        "free",
        "+2 coins  +1 water",
        "1 coin + 2 water\nor koi market",
        "2 borders\nor 2 leaves"
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
        name.setFont(Theme.title(type.length() > 7 ? 10.5 : 13));   // "WAREHOUSE" altrimenti esce dalla carta
        name.setFill(Theme.INK);

        HBox header = new HBox(8, KoiArt.seal(kanjiOf(type), type.substring(0, 1), 30, colorOf(type)), name);
        header.setAlignment(Pos.CENTER_LEFT);
        StackPane headerBox = new StackPane(band, header);
        StackPane.setMargin(header, new Insets(0, 0, 0, 8));

        Text text = new Text(type.equals("Rain") ? rainTiles(body) : prettify(body));
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

    // "[PLACEWATER] usesLeft: 2/2" -> "place up to 2 water"
    static String prettify(String body) {
        return body.replaceAll("\\s*usesLeft:.*", "")
                .replace("[", "").replace("]", "")
                .replace("PLACEWATER", "place up to 2 water")
                .replace("PLACELEAFS", "place a leaf")
                .replace("PLACELILY", "add a lily")
                .replace("PLACEBORDER", "place a border")
                .replace("PLACELANTERN", "add a lantern")
                .replace("PLACEKOI", "place a koi")
                .replace("MOVEKOI", "move a koi")
                .replace(", ", "\nor ")
                .replace("BIGKOI", "big koi")
                .replace("MEDIUMKOI", "medium koi")
                .replace("SMALLKOI", "small koi")
                .replace("WATER", "water")
                .replace("LEAF", "leaf");
    }

    // "[WATER, WATER, LEAF]" -> "2 water + 1 leaf, placed at once"
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
        return text + "\nplaced at once";
    }

    static String kanjiOf(String type) {
        switch (type) {
            case "Helper":    return "助";
            case "Warehouse": return "蔵";
            case "Market":    return "市";
            case "Rain":      return "雨";
            case "Well":      return "井";
            case "Scroll":    return "巻";
            default:          return "札";
        }
    }

    static Color colorOf(String type) {
        switch (type) {
            case "Helper":    return Theme.TEAL_DEEP;
            case "Warehouse": return Theme.WOOD;
            case "Market":    return Theme.GOLD;
            case "Rain":      return Color.web("#4a78b5");
            case "Well":      return Theme.STONE_DARK;
            case "Scroll":    return Theme.VERMILION;
            default:          return Theme.INK;
        }
    }
}
