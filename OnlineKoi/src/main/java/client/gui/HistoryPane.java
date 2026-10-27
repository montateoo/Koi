package client.gui;

import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import network.MatchRecord;
import network.PlayerView;

// storico delle partite finite: a sinistra l'elenco (stanza, data, vincitore), a destra la partita scelta
// con la classifica e lo stagno finale di ogni giocatore
final class HistoryPane extends StackPane {

    private final ListView<MatchRecord> matches = new ListView<>();
    private final VBox details = new VBox(14);
    private final Label status = new Label();

    HistoryPane(Runnable onBack) {
        getStyleClass().add("lobby-root");   // lo stesso laghetto velato della lista stanze

        Region veil = new Region();
        veil.setStyle("-fx-background-color: rgba(8, 24, 24, 0.45);");
        veil.setMouseTransparent(true);

        Text kanji = new Text(Theme.hasJapanese() ? "記" : "H");
        kanji.setFont(Theme.kanji(48));
        kanji.setFill(Theme.VERMILION);
        kanji.setEffect(new DropShadow(10, Color.color(0.5, 0.1, 0.05, 0.3)));
        Text heading = new Text("Storico partite");
        heading.setFont(Theme.title(28));
        heading.setFill(Theme.INK);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button back = new Button("← Menu");
        back.getStyleClass().add("wood-button");
        back.setMinWidth(Region.USE_PREF_SIZE);
        back.setOnAction(e -> onBack.run());
        HBox top = new HBox(12, kanji, heading, spacer, back);
        top.setAlignment(Pos.CENTER_LEFT);

        matches.setPrefWidth(300);
        matches.setPlaceholder(new Label("Nessuna partita finita"));
        matches.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(MatchRecord match, boolean empty) {
                super.updateItem(match, empty);
                if (empty || match == null) {
                    setText(null);
                } else {
                    setText(match.roomName() + "\n" + match.endedAt() + "  ·  vince " + match.winner().name());
                }
            }
        });
        matches.getSelectionModel().selectedItemProperty().addListener((obs, old, match) -> showMatch(match));

        ScrollPane detailScroll = new ScrollPane(details);
        detailScroll.setFitToWidth(true);
        detailScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        detailScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        HBox.setHgrow(detailScroll, Priority.ALWAYS);
        details.setPadding(new Insets(0, 8, 0, 4));

        HBox body = new HBox(18, matches, detailScroll);
        VBox.setVgrow(body, Priority.ALWAYS);

        status.getStyleClass().add("muted");
        VBox card = new VBox(12, top, body, status);
        card.getStyleClass().add("koi-panel");
        card.setMaxSize(1100, 620);
        card.setPrefSize(1100, 620);

        getChildren().addAll(veil, card);
        showMatch(null);
    }

    // mentre si aspetta la risposta del server
    void showLoading() {
        matches.getItems().clear();
        status.setText("Carico lo storico...");
    }

    void update(List<MatchRecord> list) {
        matches.getItems().setAll(list);
        status.setText(list.isEmpty() ? "" : list.size() + (list.size() == 1 ? " partita" : " partite"));
        if (!list.isEmpty()) {
            matches.getSelectionModel().selectFirst();
        } else {
            showMatch(null);
        }
    }

    void setStatus(String text) {
        status.setText(text);
    }

    private void showMatch(MatchRecord match) {
        details.getChildren().clear();
        if (match == null) {
            Label hint = new Label("Scegli una partita dall'elenco per vedere punteggi e stagni.");
            hint.getStyleClass().add("muted");
            details.getChildren().add(hint);
            return;
        }
        Label title = new Label(match.roomName());
        title.setFont(Theme.title(22));
        Label when = new Label(match.endedAt() + "  ·  vince " + match.winner().name()
                + " con " + match.winner().total() + " punti");
        when.getStyleClass().add("muted");

        Label pondsTitle = new Label("Gli stagni finali");
        pondsTitle.getStyleClass().add("panel-title");
        FlowPane ponds = new FlowPane(14, 14);
        // di default FlowPane va a capo dopo 400 pixel: lo si fa andare a capo solo quando finisce lo spazio
        ponds.prefWrapLengthProperty().bind(details.widthProperty().subtract(12));
        for (PlayerView player : match.players()) {
            ponds.getChildren().add(pondCard(match, player));
        }

        details.getChildren().addAll(new VBox(2, title, when), FinalScoresPane.scoreTable(match.ranking(), null),
                pondsTitle, ponds);
    }

    // lo stagno di un giocatore, ridotto, con il nome e i punti
    private static VBox pondCard(MatchRecord match, PlayerView player) {
        int total = match.ranking().stream().filter(s -> s.name().equals(player.name()))
                .mapToInt(s -> s.total()).findFirst().orElse(0);
        Label name = new Label(player.name() + "  ·  " + total + " punti");
        name.setStyle("-fx-font-weight: bold;");
        PondPane pond = new PondPane(14, false);
        pond.setAutoFit(true);
        pond.setPrefSize(270, 200);
        pond.setMinSize(270, 200);
        pond.show(player.pond(), List.of());
        VBox card = new VBox(6, name, pond);
        card.setPadding(new Insets(8));
        card.setStyle("-fx-background-color: rgba(195,236,241,0.35); -fx-background-radius: 6;");
        return card;
    }
}
