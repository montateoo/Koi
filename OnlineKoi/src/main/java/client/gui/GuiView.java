package client.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import game.scoring.ScoreSheet;
import network.CellView;
import network.GameView;
import network.HexCoord;
import network.MatchRecord;
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

    // schermata iniziale
    private TitleScreen title;
    private Scene titleScene;

    // lista delle stanze
    private final ListView<String> roomList = new ListView<>();
    private final Label lobbyPlayer = new Label();
    private final Label lobbyStatus = new Label();
    private Scene lobbyScene;

    // stanza
    private final Label roomTitle = new Label();
    private final Label turnStatus = new Label();
    private final Label hint = new Label();
    private final BoardPane board = new BoardPane();
    private final PondPane myPond = new PondPane(40, true);
    private final PlayerPanel playerPanel = new PlayerPanel();
    private final ObjectivesPane objectives = new ObjectivesPane();
    private final VBox opponents = new VBox(14);
    private final TextArea roomLog = new TextArea();
    private final StackPane questionLayer = new StackPane();
    private final StackPane scoresLayer = new StackPane();   // classifica finale: separata dalle domande, che ogni aggiornamento chiude
    private final Button scoresButton = new Button("Classifica");
    private final Button startButton = new Button("Inizia");
    private final Button endTurnButton = new Button("Passa il turno");
    private final Button homeButton = new Button("Menu");     // a fine partita, al posto di Inizia e Passa il turno

    // storico partite
    private HistoryPane historyPane;
    private Scene historyScene;
    private List<ScoreSheet> finalRanking;                   // null finche' la partita non e' finita
    private final Label banner = new Label();
    private Scene roomScene;

    // stato dell'interfaccia
    private GameView lastView;
    private String selectedType;      // tessera scelta, in attesa del clic su un esagono evidenziato
    private int rotationIndex;        // quale delle rotazioni valide si sta guardando (tasto R)

    public GuiView(Stage stage) {
        this.stage = stage;
        buildTitleScene();
        buildLobbyScene();
        buildRoomScene();
        stage.setScene(titleScene);
    }

    // da chiamare dopo stage.show(), cosi' l'animazione d'apertura si vede dall'inizio
    public void startIntro() {
        title.playIntro();
    }

    public void setController(ClientController controller) {
        this.controller = controller;
    }

    public void inBackground(Runnable task) {
        network.execute(task);
    }

    // ================================================================= schermata iniziale

    private void buildTitleScene() {
        title = new TitleScreen();
        title.setOnPlay(this::openRoomList);
        title.setOnCreateRoom(roomName -> inBackground(() -> controller.createAndJoin(roomName, Settings.nickname())));
        title.setOnHistory(this::openHistory);
        title.setOnServerChanged(() -> inBackground(() -> {
            if (controller.connect(Settings.host(), Settings.port())) {
                showMessage("Connesso a " + Settings.host() + ":" + Settings.port());
            }
        }));
        titleScene = new Scene(title, 1280, 720);
        titleScene.getStylesheets().add(stylesheet());
    }

    private void openHistory() {
        if (historyPane == null) {
            historyPane = new HistoryPane(() -> {
                stage.setScene(titleScene);
                title.showMenuNow();
            });
            historyScene = new Scene(historyPane, 1280, 720);
            historyScene.getStylesheets().add(stylesheet());
        }
        historyPane.showLoading();
        stage.setScene(historyScene);
        inBackground(controller::requestHistory);
    }

    // fine partita: si esce dalla stanza (la connessione resta) e si torna al menu
    private void goHome() {
        inBackground(controller::leaveRoom);
        scoresLayer.setVisible(false);
        stage.setScene(titleScene);
        title.showMenuNow();
    }

    private void openRoomList() {
        lobbyPlayer.setText("Giochi come " + Settings.nickname());
        lobbyStatus.setText("");
        stage.setScene(lobbyScene);
        inBackground(controller::refreshRooms);
    }

    // ================================================================= lista delle stanze

    private void buildLobbyScene() {
        roomList.setPlaceholder(new Label("Nessuna stanza: creane una dal menu"));
        roomList.setPrefHeight(220);
        lobbyPlayer.getStyleClass().add("muted");
        lobbyStatus.getStyleClass().add("muted");
        lobbyStatus.setWrapText(true);

        Button back = new Button("← Menu");
        back.getStyleClass().add("wood-button");
        back.setOnAction(e -> {
            stage.setScene(titleScene);
            title.showMenuNow();
        });

        Button refresh = new Button("Aggiorna");
        refresh.getStyleClass().add("wood-button");
        refresh.setOnAction(e -> inBackground(controller::refreshRooms));

        Button join = new Button("Entra");
        join.getStyleClass().add("wood-button");
        join.setDefaultButton(true);
        join.setOnAction(e -> joinSelected());
        roomList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                joinSelected();
            }
        });

        Text kanji = new Text(Theme.hasJapanese() ? "鯉" : "K");
        kanji.setFont(Theme.kanji(56));
        kanji.setFill(Theme.VERMILION);
        kanji.setEffect(new DropShadow(10, Color.color(0.5, 0.1, 0.05, 0.3)));
        Text heading = new Text("Stanze");
        heading.setFont(Theme.title(30));
        heading.setFill(Theme.INK);
        HBox top = new HBox(12, kanji, heading);
        top.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        for (Button button : new Button[] {back, refresh, join}) {
            button.setMinWidth(Region.USE_PREF_SIZE);   // la targa non si stringe fino a tagliare la scritta
        }
        HBox actions = new HBox(8, back, spacer, refresh, join);
        actions.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(10, top, lobbyPlayer, roomList, actions, lobbyStatus);
        card.getStyleClass().add("koi-panel");
        card.setMaxWidth(480);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        Region veil = new Region();
        veil.setStyle("-fx-background-color: rgba(8, 24, 24, 0.45);");
        veil.setMouseTransparent(true);
        StackPane root = new StackPane(veil, new SakuraLayer(12), card);
        root.getStyleClass().add("lobby-root");
        lobbyScene = new Scene(root, 1280, 720);
        lobbyScene.getStylesheets().add(stylesheet());
    }

    private void joinSelected() {
        String roomId = roomList.getSelectionModel().getSelectedItem();
        if (roomId == null) {
            lobbyStatus.setText("Scegli una stanza dalla lista");
        } else {
            inBackground(() -> controller.joinRoom(roomId, Settings.nickname()));
        }
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

        Button start = startButton;
        start.setOnAction(e -> inBackground(controller::startGame));
        Button endTurn = endTurnButton;
        endTurn.setOnAction(e -> {
            select(null);
            inBackground(controller::endTurn);
        });
        Button quit = new Button("Esci");
        quit.getStyleClass().add("ghost");
        quit.setOnAction(e -> inBackground(controller::quit));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        scoresButton.getStyleClass().add("ghost");
        scoresButton.setVisible(false);          // compare a fine partita, per riaprire la classifica
        scoresButton.managedProperty().bind(scoresButton.visibleProperty());
        scoresButton.setOnAction(e -> openScores());

        homeButton.setVisible(false);
        homeButton.setOnAction(e -> goHome());
        for (Button button : new Button[] {start, endTurn, homeButton}) {
            button.managedProperty().bind(button.visibleProperty());   // nascosti non occupano spazio
        }

        HBox header = new HBox(14, logo, roomTitle, turnStatus, spacer, scoresButton, start, endTurn, homeButton, quit);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("panel");
        header.setPadding(new Insets(8, 16, 8, 16));

        // azioni dell'interfaccia verso il controller
        board.setOnCardClicked(position -> {
            select(null);
            inBackground(() -> controller.drawCard(position));
        });
        playerPanel.setOnTileSelected(this::select);
        playerPanel.setOnSpendLucky(() -> inBackground(controller::spendLuckyCard));
        myPond.setOnHexClicked(this::placeSelected);
        myPond.setOnKoiClicked(id -> select(GameView.moveKey(id)));
        myPond.setGhostShape(this::ghostCells);
        myPond.setAutoFit(true);
        myPond.enableZoomAndPan();
        Tooltip.install(myPond, new Tooltip("Rotellina: zoom   ·   trascina: sposta   ·   doppio clic: vista iniziale"));

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
        Label othersTitle = new Label("Altri stagni");
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
        Label logTitle = new Label("Messaggi");
        logTitle.getStyleClass().add("panel-title");
        VBox log = new VBox(8, logTitle, roomLog);
        log.getStyleClass().add("panel");

        Label objectivesTitle = new Label("Obiettivi");
        objectivesTitle.getStyleClass().add("panel-title");
        objectives.update(List.of());
        VBox objectivesBox = new VBox(8, objectivesTitle, objectives);
        objectivesBox.getStyleClass().add("panel");

        VBox right = new VBox(12, objectivesBox, others, log);
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
        scoresLayer.setVisible(false);

        StackPane root = new StackPane(
                new WavePattern(Theme.PAPER, Theme.TEAL.deriveColor(0, 1, 1, 0.15)),
                main,
                new SakuraLayer(12),
                questionLayer,
                scoresLayer);
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
        // le carpe con almeno una destinazione si possono cliccare per spostarle
        Set<Integer> movable = new HashSet<>();
        if (placementQuestion().isEmpty()) {
            for (String key : lastView.playablePositions().keySet()) {
                if (GameView.isMoveKey(key)) {
                    movable.add(GameView.koiIdOf(key));
                }
            }
        }
        int selectedKoi = GameView.isMoveKey(selectedType) ? GameView.koiIdOf(selectedType) : -1;
        myPond.setMovableKoi(movable, selectedKoi);
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
        if (finalRanking != null) {
            hint.setText("La partita è finita: \"Classifica\" mostra di nuovo i punteggi.");
        } else if (lastView == null) {
            hint.setText("Premi Inizia quando ci sono tutti.");
        } else if (!lastView.isMyTurn()) {
            hint.setText(lastView.question() != null ? lastView.question() : "Tocca a " + lastView.activePlayer() + "...");
        } else if (!placementQuestion().isEmpty()) {
            hint.setText(lastView.question() + "   ·   clicca un esagono evidenziato");
        } else if (lastView.question() != null) {
            hint.setText(lastView.question());
        } else if (selectedType != null) {
            String action = GameView.isMoveKey(selectedType) ? "spostare: " : "piazzare: ";
            hint.setText("Clicca un esagono evidenziato per " + action + PlayerPanel.readable(shapeOf(selectedType)).toLowerCase()
                    + (rotatable(shapeOf(selectedType)) ? "   ·   R per ruotare" : "") + "   ·   Esc per annullare");
        } else {
            hint.setText("Medita: prendi una carta dal tabellone.   Lavora: scegli una tessera dal magazzino, o clicca una carpa per spostarla.   Poi passa il turno.");
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

    // tipo di tessera di cui disegnare la forma: per uno spostamento, la taglia della carpa scelta
    private String shapeOf(String type) {
        if (!GameView.isMoveKey(type) || lastView == null) {
            return type;
        }
        int koiId = GameView.koiIdOf(type);
        for (CellView cell : me(lastView).pond()) {
            if (cell.topId() == koiId) {
                return cell.top();
            }
        }
        return "SMALLKOI";
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
        switch (shapeOf(selectedType)) {
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
        if (GameView.isMoveKey(type)) {
            int koiId = GameView.koiIdOf(type);
            inBackground(() -> controller.moveKoi(koiId, q, r, rotation));
            return;
        }
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
            roomList.getItems().setAll(rooms);   // aggiorna soltanto: si arriva qui dal menu con "Gioca"
        });
    }

    @Override
    public void showRoom(String roomId) {
        Platform.runLater(() -> {
            roomTitle.setText("Stanza " + roomId);
            turnStatus.setText("In attesa che tutti premano Inizia");
            lastView = null;
            selectedType = null;
            finalRanking = null;
            scoresButton.setVisible(false);
            scoresLayer.setVisible(false);
            startButton.setVisible(true);
            endTurnButton.setVisible(true);
            homeButton.setVisible(false);
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
        Platform.runLater(() -> show("Errore: " + text));
    }

    @Override
    public void showGame(GameView view) {
        Platform.runLater(() -> {
            boolean turnChanged = lastView == null || !Objects.equals(lastView.activePlayer(), view.activePlayer());
            lastView = view;
            if (selectedType != null && (view.positionsFor(selectedType).isEmpty() || !placementQuestion().isEmpty())) {
                selectedType = null;
            }
            turnStatus.setText(finalRanking != null ? "Partita finita"
                    : view.isMyTurn() ? "Tocca a te" : "Tocca a " + view.activePlayer());
            board.update(view.board(), view.canDraw());
            playerPanel.update(me(view), view.playablePositions().keySet(), selectedType);
            objectives.update(view.objectives());
            refreshPond();
            refreshOpponents(view);
            refreshQuestion(view);
            updateHint();
            if (turnChanged && view.activePlayer() != null && finalRanking == null) {
                showBanner(view.isMyTurn() ? "Tocca a te" : "Tocca a " + view.activePlayer());
            }
        });
    }

    @Override
    public void showGameOver(List<ScoreSheet> ranking) {
        Platform.runLater(() -> {
            finalRanking = ranking;
            turnStatus.setText("Partita finita");
            startButton.setVisible(false);
            endTurnButton.setVisible(false);
            homeButton.setVisible(true);
            select(null);
            updateHint();
            scoresButton.setVisible(true);
            openScores();
        });
    }

    private void openScores() {
        if (finalRanking == null) {
            return;
        }
        String viewer = lastView != null ? lastView.viewer() : Settings.nickname();
        scoresLayer.getChildren().setAll(new FinalScoresPane(finalRanking, viewer,
                () -> scoresLayer.setVisible(false),
                this::goHome));
        scoresLayer.setVisible(true);
    }

    @Override
    public void showHistory(List<MatchRecord> matches) {
        Platform.runLater(() -> {
            if (historyPane != null) {
                historyPane.update(matches);
            }
        });
    }

    private void show(String text) {
        if (stage.getScene() == roomScene) {
            roomLog.appendText(text + "\n");
        } else if (stage.getScene() == titleScene) {
            title.showToast(text);
        } else if (historyScene != null && stage.getScene() == historyScene) {
            historyPane.setStatus(text);
        } else {
            lobbyStatus.setText(text);
        }
    }
}
