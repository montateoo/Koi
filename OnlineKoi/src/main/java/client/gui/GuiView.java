package client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import client.ClientController;
import client.View;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PathTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import network.GameView;
import network.HexCoord;
import network.PlayerView;

public class GuiView implements View {

    private static final String STYLESHEET = "/css/koi.css";
    private static final Pattern COORDINATE = Pattern.compile("\\((-?\\d+),\\s*(-?\\d+)\\)");

    private final Stage stage;
    private ClientController controller;

    // le chiamate al server non devono mai girare sul thread di JavaFX, altrimenti la finestra si blocca
    private final ExecutorService network = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "network");
        thread.setDaemon(true);
        return thread;
    });

    // lobby
    private final TextField nicknameField = new TextField();
    private final ListView<String> roomList = new ListView<>();
    private final TextField roomNameField = new TextField();
    private final Label lobbyStatus = new Label("Connecting...");
    private Scene lobbyScene;

    // stanza
    private final Label roomTitle = new Label();
    private final Label turnStatus = new Label();
    private final Label hint = new Label();
    private final BoardPane board = new BoardPane();
    private final PondPane myPond = new PondPane(40, true);
    private final PlayerPanel playerPanel = new PlayerPanel();
    private final VBox opponents = new VBox(14);
    private final TextArea roomLog = new TextArea();
    private final StackPane questionLayer = new StackPane();
    private final Label banner = new Label();
    private Scene roomScene;

    // stato dell'interfaccia
    private GameView lastView;
    private String selectedType;      // tessera scelta, in attesa del clic su un esagono evidenziato
    private int rotationIndex;        // quale delle rotazioni valide si sta guardando (tasto R)

    public GuiView(Stage stage) {
        this.stage = stage;
        buildLobbyScene();
        buildRoomScene();
        stage.setScene(lobbyScene);
    }

    public void setController(ClientController controller) {
        this.controller = controller;
    }

    public void inBackground(Runnable task) {
        network.execute(task);
    }

    // ================================================================= lobby

    private void buildLobbyScene() {
        nicknameField.setPromptText("Your nickname");
        roomNameField.setPromptText("New room name");
        roomList.setPlaceholder(new Label("No rooms yet"));
        roomList.setPrefHeight(160);
        lobbyStatus.getStyleClass().add("muted");

        Button refresh = new Button("Refresh");
        refresh.getStyleClass().add("ghost");
        refresh.setOnAction(e -> inBackground(controller::refreshRooms));

        Button join = new Button("Join room");
        join.setOnAction(e -> {
            String nickname = nicknameField.getText().trim();
            String roomId = roomList.getSelectionModel().getSelectedItem();
            if (nickname.isEmpty()) {
                showError("Enter a nickname first");
            } else if (roomId == null) {
                showError("Select a room first");
            } else {
                inBackground(() -> controller.joinRoom(roomId, nickname));
            }
        });

        Button create = new Button("Create");
        create.getStyleClass().add("ghost");
        create.setOnAction(e -> {
            String roomName = roomNameField.getText().trim();
            if (roomName.isEmpty()) {
                showError("Enter a room name first");
            } else {
                inBackground(() -> controller.createRoom(roomName));
            }
        });

        Text kanji = new Text(Theme.hasJapanese() ? "鯉" : "K");
        kanji.setFont(Theme.kanji(84));
        kanji.setFill(Theme.VERMILION);
        kanji.setEffect(new DropShadow(10, Color.color(0.5, 0.1, 0.05, 0.3)));
        Text title = new Text("K O I");
        title.setFont(Theme.title(34));
        title.setFill(Theme.INK);
        Text subtitle = new Text("build the most serene pond for your koi");
        subtitle.setFont(Theme.serif(14));
        subtitle.setFill(Theme.INK_SOFT);
        VBox heading = new VBox(0, kanji, title, subtitle);
        heading.setAlignment(Pos.CENTER);

        HBox.setHgrow(roomNameField, Priority.ALWAYS);
        VBox card = new VBox(10, heading, new Label("Nickname"), nicknameField, new Label("Rooms"), roomList,
                new HBox(8, join, refresh), new HBox(8, roomNameField, create), lobbyStatus);
        card.getStyleClass().add("panel");
        card.setPadding(new Insets(22, 28, 22, 28));
        card.setMaxWidth(420);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        StackPane root = new StackPane(
                new WavePattern(Theme.PAPER, Theme.TEAL.deriveColor(0, 1, 1, 0.2)),
                swimmingKoi(),
                card,
                new SakuraLayer(18));
        lobbyScene = new Scene(root, 960, 680);
        lobbyScene.getStylesheets().add(stylesheet());
    }

    // carpe decorative che attraversano lo sfondo della lobby lungo curve morbide
    private Node swimmingKoi() {
        Pane pond = new Pane();
        pond.setMouseTransparent(true);
        double[][] routes = {
            {-120, 520, 250, 360, 600, 640, 1100, 420},
            {1100, 160, 760, 40, 330, 300, -140, 120},
            {-120, 260, 300, 120, 650, 380, 1100, 250}
        };
        for (int i = 0; i < routes.length; i++) {
            double[] p = routes[i];
            Group fish = KoiArt.koi(80 + i * 14, i);
            fish.setOpacity(0.85);
            Path path = new Path(new MoveTo(p[0], p[1]), new CubicCurveTo(p[2], p[3], p[4], p[5], p[6], p[7]));
            PathTransition swim = new PathTransition(Duration.seconds(16 + i * 5), path, fish);
            swim.setOrientation(PathTransition.OrientationType.ORTHOGONAL_TO_TANGENT);
            swim.setInterpolator(Interpolator.LINEAR);
            swim.setCycleCount(Animation.INDEFINITE);
            swim.setDelay(Duration.seconds(i * 3.5));
            swim.play();
            pond.getChildren().add(fish);
        }
        return pond;
    }

    // ================================================================= stanza

    private void buildRoomScene() {
        roomTitle.setFont(Theme.title(18));
        turnStatus.getStyleClass().add("status");
        hint.getStyleClass().add("hint");
        hint.setWrapText(true);

        Text logo = new Text(Theme.hasJapanese() ? "鯉" : "K");
        logo.setFont(Theme.kanji(32));
        logo.setFill(Theme.VERMILION);

        Button start = new Button("Start game");
        start.setOnAction(e -> inBackground(controller::startGame));
        Button endTurn = new Button("End turn");
        endTurn.setOnAction(e -> {
            select(null);
            inBackground(controller::endTurn);
        });
        Button quit = new Button("Quit");
        quit.getStyleClass().add("ghost");
        quit.setOnAction(e -> inBackground(controller::quit));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(14, logo, roomTitle, turnStatus, spacer, start, endTurn, quit);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("panel");
        header.setPadding(new Insets(8, 16, 8, 16));

        // azioni dell'interfaccia verso il controller
        board.setOnCardClicked(position -> {
            select(null);
            inBackground(() -> controller.drawCard(position));
        });
        playerPanel.setOnTileSelected(this::select);
        myPond.setOnHexClicked(this::placeSelected);
        myPond.setGhostShape(this::ghostCells);
        myPond.setAutoFit(true);
        myPond.enableZoomAndPan();
        Tooltip.install(myPond, new Tooltip("Mouse wheel: zoom   ·   drag: move   ·   double-click: reset view"));

        // centro: tabellone sopra, stagno sotto, suggerimento in fondo
        StackPane boardBox = new StackPane(board);
        boardBox.getStyleClass().add("panel");

        banner.setFont(Theme.title(46));
        banner.setStyle("-fx-text-fill: #c8341f;");   // stile inline: la regola .label del CSS altrimenti la renderebbe nera
        banner.setEffect(new DropShadow(16, Color.color(1, 1, 1, 0.9)));
        banner.setMouseTransparent(true);
        banner.setOpacity(0);

        StackPane pondArea = new StackPane(myPond, banner);
        pondArea.getStyleClass().add("panel");
        pondArea.setStyle("-fx-background-color: rgba(246,239,220,0.6);");
        VBox.setVgrow(pondArea, Priority.ALWAYS);
        VBox centre = new VBox(12, boardBox, pondArea, hint);

        // sinistra: la propria plancia
        ScrollPane left = new ScrollPane(playerPanel);
        left.setFitToWidth(true);
        left.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // destra: gli altri stagni e i messaggi
        Label othersTitle = new Label("Other ponds");
        othersTitle.getStyleClass().add("panel-title");
        ScrollPane othersScroll = new ScrollPane(opponents);
        othersScroll.setFitToWidth(true);
        VBox others = new VBox(8, othersTitle, othersScroll);
        others.getStyleClass().add("panel");
        VBox.setVgrow(othersScroll, Priority.ALWAYS);
        VBox.setVgrow(others, Priority.ALWAYS);

        roomLog.setEditable(false);
        roomLog.setWrapText(true);
        roomLog.setPrefRowCount(9);
        Label logTitle = new Label("Messages");
        logTitle.getStyleClass().add("panel-title");
        VBox log = new VBox(8, logTitle, roomLog);
        log.getStyleClass().add("panel");

        VBox right = new VBox(12, others, log);
        right.setPrefWidth(300);
        right.setMinWidth(260);

        BorderPane main = new BorderPane();
        main.setTop(header);
        main.setLeft(left);
        main.setCenter(centre);
        main.setRight(right);
        BorderPane.setMargin(header, new Insets(0, 0, 12, 0));
        BorderPane.setMargin(left, new Insets(0, 12, 0, 0));
        BorderPane.setMargin(right, new Insets(0, 0, 0, 12));
        main.setPadding(new Insets(14));

        questionLayer.setVisible(false);

        StackPane root = new StackPane(
                new WavePattern(Theme.PAPER, Theme.TEAL.deriveColor(0, 1, 1, 0.15)),
                main,
                new SakuraLayer(12),
                questionLayer);
        // impaginato per 1340x780: su schermi piu' piccoli (o con il ridimensionamento di Windows) tutto si riduce insieme
        roomScene = new Scene(new FitToWindow(root, 1340, 780), 1360, 880);
        roomScene.getStylesheets().add(stylesheet());
        roomScene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.R) {
                rotationIndex++;
                myPond.refreshGhost();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                select(null);
            }
        });
    }

    private String stylesheet() {
        return Objects.requireNonNull(GuiView.class.getResource(STYLESHEET), "missing " + STYLESHEET).toExternalForm();
    }

    // ================================================================= aggiornamenti dalla partita

    private PlayerView me(GameView view) {
        for (PlayerView player : view.players()) {
            if (player.name().equals(view.viewer())) {
                return player;
            }
        }
        return view.players().get(0);
    }

    private void refreshPond() {
        if (lastView == null) {
            return;
        }
        List<HexCoord> spots;
        if (!placementQuestion().isEmpty()) {
            spots = placementQuestion();          // una domanda "dove piazzi?" ha la precedenza sulla selezione
        } else if (selectedType != null) {
            spots = lastView.positionsFor(selectedType);
        } else {
            spots = List.of();
        }
        myPond.show(me(lastView).pond(), spots);
    }

    // se la domanda aperta ha come opzioni solo coordinate come "(1,-2)", si risponde cliccando lo stagno:
    // restituisce quelle coordinate nell'ordine delle opzioni, altrimenti una lista vuota
    private List<HexCoord> placementQuestion() {
        if (lastView == null || lastView.question() == null || lastView.options().isEmpty()) {
            return List.of();
        }
        List<HexCoord> coords = new ArrayList<>();
        for (String option : lastView.options()) {
            Matcher match = COORDINATE.matcher(option.trim());
            if (!match.matches()) {
                return List.of();
            }
            coords.add(new HexCoord(Integer.parseInt(match.group(1)), Integer.parseInt(match.group(2))));
        }
        return coords;
    }

    private void refreshOpponents(GameView view) {
        opponents.getChildren().clear();
        for (PlayerView player : view.players()) {
            if (player.name().equals(view.viewer())) {
                continue;
            }
            PondPane mini = new PondPane(13, false);
            mini.show(player.pond(), List.of());
            VBox box = new VBox(6, PlayerPanel.summary(player), mini);
            box.setPadding(new Insets(6));
            box.setStyle("-fx-background-color: rgba(195,236,241,0.25); -fx-background-radius: 6;");
            opponents.getChildren().add(box);
        }
    }

    private void refreshQuestion(GameView view) {
        // nessuna domanda, oppure una domanda di piazzamento che si risolve cliccando lo stagno: niente finestra
        if (view.question() == null || view.options().isEmpty() || !placementQuestion().isEmpty()) {
            questionLayer.setVisible(false);
            questionLayer.getChildren().clear();
            return;
        }
        Region dim = new Region();
        dim.setStyle("-fx-background-color: rgba(30, 20, 10, 0.45);");

        Text question = new Text(view.question());
        question.setFont(Theme.title(20));
        question.setFill(Theme.INK);
        question.setWrappingWidth(380);

        VBox buttons = new VBox(8);
        for (int i = 0; i < view.options().size(); i++) {
            int option = i;
            Button answer = new Button(view.options().get(i));
            answer.setMaxWidth(Double.MAX_VALUE);
            answer.setOnAction(e -> {
                questionLayer.setVisible(false);
                inBackground(() -> controller.choose(option));
            });
            buttons.getChildren().add(answer);
        }

        HBox top = new HBox(12, KoiArt.seal("問", "?", 36, Theme.VERMILION), question);
        top.setAlignment(Pos.CENTER_LEFT);
        VBox panel = new VBox(16, top, buttons);
        panel.getStyleClass().add("panel");
        panel.setPadding(new Insets(22));
        panel.setMaxSize(460, Region.USE_PREF_SIZE);

        questionLayer.getChildren().setAll(dim, panel);
        if (!questionLayer.isVisible()) {
            questionLayer.setVisible(true);
            panel.setScaleX(0.85);
            panel.setScaleY(0.85);
            ScaleTransition grow = new ScaleTransition(Duration.millis(200), panel);
            grow.setToX(1.04);
            grow.setToY(1.04);
            grow.setInterpolator(Interpolator.EASE_OUT);
            ScaleTransition settle = new ScaleTransition(Duration.millis(120), panel);
            settle.setToX(1);
            settle.setToY(1);
            FadeTransition fade = new FadeTransition(Duration.millis(220), questionLayer);
            fade.setFromValue(0);
            fade.setToValue(1);
            new ParallelTransition(new SequentialTransition(grow, settle), fade).play();
        }
    }

    private void updateHint() {
        if (lastView == null) {
            hint.setText("Press Start when everyone has joined.");
        } else if (!lastView.isMyTurn()) {
            hint.setText(lastView.question() != null ? lastView.question() : "Waiting for " + lastView.activePlayer() + "...");
        } else if (!placementQuestion().isEmpty()) {
            hint.setText(lastView.question() + "   ·   click a highlighted hex");
        } else if (lastView.question() != null) {
            hint.setText(lastView.question());
        } else if (selectedType != null) {
            hint.setText("Click a highlighted hex to place the " + PlayerPanel.readable(selectedType).toLowerCase()
                    + (rotatable(selectedType) ? "   ·   R to rotate" : "") + "   ·   Esc to cancel");
        } else {
            hint.setText("Meditate: take a card from the board.   Work: pick a tile from your storage.   Then end your turn.");
        }
    }

    private void showBanner(String text) {
        banner.setText(text);
        FadeTransition in = new FadeTransition(Duration.millis(350), banner);
        in.setFromValue(0);
        in.setToValue(1);
        ScaleTransition grow = new ScaleTransition(Duration.millis(350), banner);
        grow.setFromX(0.7);
        grow.setFromY(0.7);
        grow.setToX(1);
        grow.setToY(1);
        FadeTransition out = new FadeTransition(Duration.millis(700), banner);
        out.setToValue(0);
        new SequentialTransition(new ParallelTransition(in, grow), new PauseTransition(Duration.seconds(1.1)), out).play();
    }

    // ================================================================= selezione e piazzamento

    private void select(String type) {
        selectedType = (type == null || type.equals(selectedType)) ? null : type;
        rotationIndex = 0;
        if (lastView != null) {
            playerPanel.update(me(lastView), lastView.playablePositions().keySet(), selectedType);
        }
        refreshPond();
        updateHint();
    }

    private static boolean rotatable(String type) {
        return type.equals("BORDER") || type.equals("MEDIUMKOI") || type.equals("BIGKOI");
    }

    private int currentRotation(HexCoord anchor) {
        List<Integer> rotations = lastView.rotationsAt(selectedType, anchor);
        return rotations.isEmpty() ? 0 : rotations.get(Math.floorMod(rotationIndex, rotations.size()));
    }

    private static HexCoord neighbour(HexCoord coord, int direction) {
        int d = Math.floorMod(direction, 6);
        return new HexCoord(coord.q() + PondPane.DQ[d], coord.r() + PondPane.DR[d]);
    }

    // stesse forme di Pond: sponda = centro + ali in 4 e 0; carpa media = +0; carpa grande = +0 e +1 (ruotate)
    private List<HexCoord> ghostCells(HexCoord anchor) {
        if (selectedType == null || lastView == null) {
            return List.of(anchor);
        }
        int rotation = currentRotation(anchor);
        switch (selectedType) {
            case "BORDER":
                return List.of(anchor, neighbour(anchor, 4 + rotation), neighbour(anchor, rotation));
            case "MEDIUMKOI":
                return List.of(anchor, neighbour(anchor, rotation));
            case "BIGKOI":
                return List.of(anchor, neighbour(anchor, rotation), neighbour(anchor, rotation + 1));
            default:
                return List.of(anchor);
        }
    }

    private void placeSelected(HexCoord coord) {
        // domanda di piazzamento aperta (es. Pioggia): il clic sceglie l'opzione con quella coordinata
        List<HexCoord> questionSpots = placementQuestion();
        if (!questionSpots.isEmpty()) {
            int option = questionSpots.indexOf(coord);
            if (option >= 0) {
                inBackground(() -> controller.choose(option));
            }
            return;
        }
        if (selectedType == null || lastView == null) {
            return;
        }
        String type = selectedType;
        int q = coord.q();
        int r = coord.r();
        int rotation = currentRotation(coord);
        select(null);
        inBackground(() -> {
            switch (type) {
                case "WATER":
                    controller.placeWater(q, r);
                    break;
                case "LEAF":
                    controller.placeLeaf(q, r);
                    break;
                case "LILY":
                    controller.placeLily(q, r);
                    break;
                case "LANTERN":
                    controller.placeLantern(q, r);
                    break;
                case "BORDER":
                    controller.placeBorder(q, r, rotation);
                    break;
                case "SMALLKOI":
                    controller.placeKoi(0, q, r, 0);
                    break;
                case "MEDIUMKOI":
                    controller.placeKoi(1, q, r, rotation);
                    break;
                case "BIGKOI":
                    controller.placeKoi(2, q, r, rotation);
                    break;
                default:
                    break;
            }
        });
    }

    // ================================================================= metodi di View
    // possono arrivare dai thread di RMI: ogni modifica alla finestra passa da Platform.runLater

    @Override
    public void showLobby(List<String> rooms) {
        Platform.runLater(() -> {
            roomList.getItems().setAll(rooms);
            lobbyStatus.setText("");
            stage.setScene(lobbyScene);
        });
    }

    @Override
    public void showRoom(String roomId) {
        Platform.runLater(() -> {
            roomTitle.setText("Room " + roomId);
            turnStatus.setText("Waiting for everyone to press Start");
            lastView = null;
            selectedType = null;
            roomLog.clear();
            updateHint();
            stage.setScene(roomScene);
            stage.setMaximized(true);   // FitToWindow adatta il contenuto a qualunque dimensione dello schermo
        });
    }

    @Override
    public void showMessage(String text) {
        Platform.runLater(() -> show(text));
    }

    @Override
    public void showError(String text) {
        Platform.runLater(() -> show("Error: " + text));
    }

    @Override
    public void showGame(GameView view) {
        Platform.runLater(() -> {
            boolean turnChanged = lastView == null || !Objects.equals(lastView.activePlayer(), view.activePlayer());
            lastView = view;
            if (selectedType != null && (view.positionsFor(selectedType).isEmpty() || !placementQuestion().isEmpty())) {
                selectedType = null;
            }
            turnStatus.setText(view.isMyTurn() ? "Your turn" : "Turn of " + view.activePlayer());
            board.update(view.board(), view.canDraw());
            playerPanel.update(me(view), view.playablePositions().keySet(), selectedType);
            refreshPond();
            refreshOpponents(view);
            refreshQuestion(view);
            updateHint();
            if (turnChanged && view.activePlayer() != null) {
                showBanner(view.isMyTurn() ? "Your turn" : view.activePlayer() + "'s turn");
            }
        });
    }

    private void show(String text) {
        if (stage.getScene() == roomScene) {
            roomLog.appendText(text + "\n");
        } else {
            lobbyStatus.setText(text);
        }
    }
}
