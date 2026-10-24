package client.gui;

import java.util.List;

import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Polygon;
import network.ObjectiveView;

// i tre obiettivi sul tavolo: disegno, nome, costo, requisito e tessere rimaste;
// bordo dorato e "Claimable" quando chi guarda lo puo' reclamare a fine turno
final class ObjectivesPane extends VBox {

    private static final double ICON = 54;   // lato del riquadro del disegno, in pixel

    ObjectivesPane() {
        super(8);
    }

    void update(List<ObjectiveView> objectives) {
        getChildren().clear();
        if (objectives.isEmpty()) {
            Label waiting = new Label("Revealed when the game starts");
            waiting.getStyleClass().add("muted");
            getChildren().add(waiting);
            return;
        }
        for (ObjectiveView objective : objectives) {
            getChildren().add(card(objective));
        }
    }

    private Region card(ObjectiveView objective) {
        Label name = new Label(objective.name());
        name.setFont(Theme.title(15));

        HBox title = new HBox(6, name);
        title.setAlignment(Pos.CENTER_LEFT);
        if (objective.cost() > 0) {
            Label cost = new Label(String.valueOf(objective.cost()));
            title.getChildren().addAll(KoiArt.coin(7), cost);
        }
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        title.getChildren().add(spacer);
        if (objective.claimable()) {
            Label badge = new Label("Claimable");
            badge.setStyle("-fx-text-fill: #fbf7ec; -fx-background-color: #c8341f; -fx-background-radius: 3;"
                    + " -fx-padding: 1 5 1 5; -fx-font-size: 11px;");
            title.getChildren().add(badge);
        }

        Label requirement = new Label(objective.requirement());
        requirement.setWrapText(true);
        requirement.getStyleClass().add("muted");
        requirement.setStyle("-fx-font-size: 11px;");

        Label left = new Label(objective.tilesLeft() == 0 ? "All taken"
                : objective.tilesLeft() + (objective.tilesLeft() == 1 ? " tile left" : " tiles left"));
        left.getStyleClass().add("muted");
        left.setStyle("-fx-font-size: 11px;");

        VBox text = new VBox(2, title, requirement, left);
        HBox.setHgrow(text, Priority.ALWAYS);

        HBox card = new HBox(10, icon(objective.tileType()), text);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(6, 8, 6, 6));
        String border = objective.claimable() ? "#d4a13a" : "rgba(111,106,94,0.35)";
        card.setStyle("-fx-background-color: rgba(251,247,236,0.9); -fx-background-radius: 6;"
                + " -fx-border-color: " + border + "; -fx-border-radius: 6; -fx-border-width: "
                + (objective.claimable() ? 2 : 1) + ";");
        if (objective.tilesLeft() == 0) {
            card.setOpacity(0.5);
        }
        Tooltip.install(card, new Tooltip(objective.name() + "\n" + objective.requirement()));
        return card;
    }

    // il disegno dell'obiettivo su un esagono d'acqua, ridotto per entrare nel riquadro
    private static StackPane icon(String tileType) {
        double s = ICON * 0.42;
        Polygon hex = KoiArt.hexagon(0, 0, s);
        hex.setFill(KoiArt.waterFill(0, 0, s, false));
        hex.setStroke(Theme.TEAL.darker());
        Group drawing = new Group(hex);
        if (tileType != null) {
            drawing.getChildren().add(ObjectiveArt.objective(tileType, s, 1));
        }
        Group fitted = new Group(drawing);
        Bounds bounds = drawing.getLayoutBounds();
        double scale = Math.min(1, ICON / Math.max(bounds.getWidth(), bounds.getHeight()));
        drawing.setScaleX(scale);
        drawing.setScaleY(scale);

        StackPane box = new StackPane(fitted);
        box.setMinSize(ICON, ICON);
        box.setPrefSize(ICON, ICON);
        box.setMaxSize(ICON, ICON);
        return box;
    }
}
