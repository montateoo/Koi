package client.gui;

import java.util.List;

import game.scoring.ScoreSheet;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;

// classifica di fine partita, come il blocchetto segnapunti: una riga per giocatore, una colonna per voce.
// Il vincitore e' in oro, chi guarda in grassetto; si puo' chiudere per guardare lo stagno finale
final class FinalScoresPane extends StackPane {

    private static final String[] COLUMNS = {"", "Giocatore", "Carpe", "Lanterne", "Pergamene", "Obiettivi", "Fortuna", "Totale", "Monete"};

    FinalScoresPane(List<ScoreSheet> ranking, String viewer, Runnable onClose, Runnable onHome) {
        Region dim = new Region();
        dim.setStyle("-fx-background-color: rgba(10, 20, 20, 0.55);");

        ScoreSheet winner = ranking.get(0);
        Text kanji = new Text(Theme.hasJapanese() ? "勝" : "W");
        kanji.setFont(Theme.kanji(46));
        kanji.setFill(Theme.VERMILION);
        Label title = new Label(winner.name().equals(viewer) ? "Hai vinto!" : "Vince " + winner.name() + "!");
        title.setFont(Theme.title(28));
        Label subtitle = new Label(winner.total() + " punti");
        subtitle.getStyleClass().add("muted");
        VBox heading = new VBox(2, title, subtitle);
        HBox top = new HBox(14, kanji, heading);
        top.setAlignment(Pos.CENTER_LEFT);

        GridPane table = scoreTable(ranking, viewer);

        Label note = new Label("In caso di parità vince chi ha più monete, poi chi giocava dopo nell'ordine di turno.");
        note.getStyleClass().add("muted");
        note.setStyle("-fx-font-size: 11px;");

        Button look = new Button("Guarda lo stagno");
        look.getStyleClass().add("wood-button");
        look.setMinWidth(Region.USE_PREF_SIZE);
        look.setOnAction(e -> onClose.run());
        Button home = new Button("Menu");
        home.getStyleClass().add("wood-button");
        home.setMinWidth(Region.USE_PREF_SIZE);
        home.setOnAction(e -> onHome.run());
        HBox buttons = new HBox(10, look, home);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox panel = new VBox(14, top, table, note, buttons);
        panel.getStyleClass().add("koi-panel");
        panel.setPadding(new Insets(30, 40, 26, 40));
        panel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        getChildren().addAll(dim, panel);

        // entrata: il pannello cresce un poco mentre lo sfondo si scurisce
        panel.setScaleX(0.9);
        panel.setScaleY(0.9);
        ScaleTransition grow = new ScaleTransition(Duration.millis(320), panel);
        grow.setToX(1);
        grow.setToY(1);
        grow.setInterpolator(Interpolator.EASE_OUT);
        FadeTransition fade = new FadeTransition(Duration.millis(320), this);
        fade.setFromValue(0);
        fade.setToValue(1);
        new ParallelTransition(grow, fade).play();
    }

    // la tabella dei punti, una riga per giocatore: la usa anche lo storico delle partite
    static GridPane scoreTable(List<ScoreSheet> ranking, String viewer) {
        GridPane table = new GridPane();
        table.setHgap(14);
        table.setVgap(6);
        for (int c = 0; c < COLUMNS.length; c++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setHalignment(c <= 1 ? HPos.LEFT : HPos.RIGHT);
            if (c == 1) {
                column.setHgrow(Priority.ALWAYS);
            }
            table.getColumnConstraints().add(column);
            Label header = new Label(COLUMNS[c]);
            header.getStyleClass().add("panel-title");
            table.add(header, c, 0);
        }
        for (int i = 0; i < ranking.size(); i++) {
            ScoreSheet s = ranking.get(i);
            Object[] values = {(i + 1) + ".", s.name(), s.koi(), s.lanterns(), s.scrolls(), s.objectives(),
                    s.fortune(), s.total(), s.coins()};
            String style = (i == 0 ? "-fx-text-fill: #7a4f08;" : "")
                    + (s.name().equals(viewer) ? "-fx-font-weight: bold;" : "");
            for (int c = 0; c < values.length; c++) {
                Label cell = new Label(String.valueOf(values[c]));
                cell.setStyle(style + (c == 7 ? "-fx-font-size: 16px; -fx-font-weight: bold;" : ""));
                table.add(cell, c, i + 1);
            }
        }
        return table;
    }
}
