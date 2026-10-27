package client.gui;

import java.util.Objects;
import java.util.function.Consumer;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.util.Duration;

// schermata iniziale: il nero svanisce sul laghetto, l'immagine si avvicina piano e compare il menu.
// Un clic o un tasto durante l'introduzione la salta. In basso a destra il giocatore imposta il proprio nome
final class TitleScreen extends StackPane {

    private static final String BACKGROUND = "/assets/images/title.jpg";
    private static final double ZOOM = 1.12;

    private final Image image = new Image(Objects.requireNonNull(
            TitleScreen.class.getResourceAsStream(BACKGROUND), "missing " + BACKGROUND));
    private final ImageView background = new ImageView(image);
    private final Rectangle curtain = new Rectangle();          // il nero dell'apertura
    private final VBox menu;
    private final HBox playerCorner;
    private final Label playerName = new Label();
    private final StackPane dialogLayer = new StackPane();
    private final Label toast = new Label();
    private Animation intro;
    private Animation toastFade;

    private Runnable onPlay = () -> { };
    private Consumer<String> onCreateRoom = name -> { };
    private Runnable onServerChanged = () -> { };
    private Runnable onHistory = () -> { };

    TitleScreen() {
        // sfondo "a riempimento": copre tutta la finestra senza deformarsi, l'eccesso si taglia
        background.setManaged(false);
        background.setSmooth(true);
        widthProperty().addListener((obs, old, value) -> cover());
        heightProperty().addListener((obs, old, value) -> cover());
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);

        // una velatura scura in basso fa leggere meglio i pulsanti sopra l'acqua chiara
        Region shade = new Region();
        shade.setStyle("-fx-background-color: linear-gradient(to bottom, rgba(10,30,30,0.05), rgba(10,30,30,0.45));");
        shade.setMouseTransparent(true);

        menu = buildMenu();
        playerCorner = buildPlayerCorner();
        StackPane.setAlignment(playerCorner, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(playerCorner, new Insets(0, 28, 24, 0));

        toast.getStyleClass().add("title-toast");
        toast.setOpacity(0);
        toast.setMouseTransparent(true);
        StackPane.setAlignment(toast, Pos.BOTTOM_CENTER);
        StackPane.setMargin(toast, new Insets(0, 0, 34, 0));

        dialogLayer.setVisible(false);

        curtain.setFill(Color.BLACK);
        curtain.widthProperty().bind(widthProperty());
        curtain.heightProperty().bind(heightProperty());
        curtain.setManaged(false);

        SakuraLayer petals = new SakuraLayer(10);
        petals.setMouseTransparent(true);

        getChildren().addAll(background, shade, petals, menu, playerCorner, toast, dialogLayer, curtain);
        getStyleClass().add("title-screen");

        // un clic o un tasto durante l'introduzione la porta alla fine
        addEventFilter(MouseEvent.MOUSE_PRESSED, e -> skipIntro());
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE && dialogLayer.isVisible()) {
                closeDialog();
            } else {
                skipIntro();
            }
        });
        refreshPlayerName();
    }

    void setOnPlay(Runnable action) {
        this.onPlay = action;
    }

    void setOnCreateRoom(Consumer<String> action) {
        this.onCreateRoom = action;
    }

    void setOnHistory(Runnable action) {
        this.onHistory = action;
    }

    void setOnServerChanged(Runnable action) {
        this.onServerChanged = action;
    }

    // ---------------------------------------------------------------- introduzione

    // nero -> laghetto (2,5 s), poi lo zoom lento (6 s) durante il quale salgono il menu e il pulsante del giocatore
    void playIntro() {
        if (Settings.skipIntro()) {
            showMenuNow();
            return;
        }
        curtain.setOpacity(1);
        curtain.setVisible(true);
        background.setScaleX(1);
        background.setScaleY(1);
        hideForIntro(menu, 30);
        hideForIntro(playerCorner, 16);

        FadeTransition reveal = new FadeTransition(Duration.seconds(2.5), curtain);
        reveal.setFromValue(1);
        reveal.setToValue(0);
        reveal.setInterpolator(Interpolator.EASE_IN);

        ScaleTransition zoom = new ScaleTransition(Duration.seconds(6), background);
        zoom.setToX(ZOOM);
        zoom.setToY(ZOOM);
        zoom.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1));

        SequentialTransition menuIn = new SequentialTransition(new PauseTransition(Duration.seconds(1.8)),
                rise(menu, 1.4));
        SequentialTransition cornerIn = new SequentialTransition(new PauseTransition(Duration.seconds(3.0)),
                rise(playerCorner, 1.0));

        intro = new SequentialTransition(new PauseTransition(Duration.seconds(0.4)), reveal,
                new ParallelTransition(zoom, menuIn, cornerIn));
        intro.setOnFinished(e -> curtain.setVisible(false));
        intro.play();
    }

    // tornando dalla lista delle stanze: niente animazione, tutto al suo posto
    void showMenuNow() {
        if (intro != null) {
            intro.stop();
        }
        curtain.setOpacity(0);
        curtain.setVisible(false);
        background.setScaleX(ZOOM);
        background.setScaleY(ZOOM);
        for (Node node : new Node[] {menu, playerCorner}) {
            node.setOpacity(1);
            node.setTranslateY(0);
        }
    }

    private void skipIntro() {
        if (intro != null && intro.getStatus() == Animation.Status.RUNNING) {
            intro.jumpTo(intro.getTotalDuration());
            intro.stop();
            showMenuNow();
        }
    }

    private static void hideForIntro(Node node, double drop) {
        node.setOpacity(0);
        node.setTranslateY(drop);
    }

    private static Animation rise(Node node, double seconds) {
        FadeTransition fade = new FadeTransition(Duration.seconds(seconds), node);
        fade.setToValue(1);
        TranslateTransition up = new TranslateTransition(Duration.seconds(seconds), node);
        up.setToY(0);
        up.setInterpolator(Interpolator.EASE_OUT);
        return new ParallelTransition(fade, up);
    }

    private void cover() {
        double width = getWidth();
        double height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        double scale = Math.max(width / image.getWidth(), height / image.getHeight());
        double fitWidth = image.getWidth() * scale;
        double fitHeight = image.getHeight() * scale;
        background.setFitWidth(fitWidth);
        background.setFitHeight(fitHeight);
        background.setLayoutX((width - fitWidth) / 2);
        background.setLayoutY((height - fitHeight) / 2);
    }

    // ---------------------------------------------------------------- menu

    private VBox buildMenu() {
        Text kanji = new Text(Theme.hasJapanese() ? "鯉" : "K");
        kanji.setFont(Theme.kanji(76));
        kanji.setFill(Theme.VERMILION);
        kanji.setEffect(new DropShadow(18, Color.color(1, 1, 1, 0.75)));
        Text title = new Text("K O I");
        title.setFont(Theme.title(46));
        title.setFill(Theme.PAPER_LIGHT);
        title.setEffect(new DropShadow(14, Color.color(0.05, 0.15, 0.15, 0.85)));

        Button play = menuButton("Gioca");
        play.setDefaultButton(true);
        play.setOnAction(e -> withNickname(onPlay));
        Button create = menuButton("Crea Stanza");
        create.setOnAction(e -> withNickname(this::askRoomName));
        Button history = menuButton("Storico");
        history.setOnAction(e -> onHistory.run());
        Button settings = menuButton("Impostazioni");
        settings.setOnAction(e -> openSettings());

        VBox buttons = new VBox(12, play, create, history, settings);
        buttons.setAlignment(Pos.CENTER);

        VBox box = new VBox(6, kanji, title, spacer(18), buttons);
        box.setAlignment(Pos.CENTER);
        box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        return box;
    }

    // targa di legno (assets/ui/button.png): misura fissa con le proporzioni dell'immagine
    private static Button menuButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("menu-button");
        button.setPrefSize(300, 100);
        button.setMinSize(300, 100);
        return button;
    }

    private static Region spacer(double height) {
        Region spacer = new Region();
        spacer.setMinHeight(height);
        return spacer;
    }

    // ---------------------------------------------------------------- giocatore

    private HBox buildPlayerCorner() {
        SVGPath person = new SVGPath();
        person.setContent("M12 12a5 5 0 1 0 0-10 5 5 0 0 0 0 10zm0 2c-4.4 0-8 2.2-8 5v1h16v-1c0-2.8-3.6-5-8-5z");
        person.setFill(Theme.PAPER_LIGHT);
        person.setEffect(new DropShadow(3, Color.color(0.2, 0.1, 0.05, 0.8)));
        person.setScaleX(1.25);
        person.setScaleY(1.25);

        Button button = new Button();
        button.setGraphic(person);
        button.getStyleClass().add("player-button");
        button.setOnAction(e -> askNickname(null));
        button.setTooltip(new Tooltip("Il tuo nome"));

        playerName.getStyleClass().add("player-name");
        playerName.setOnMouseClicked(e -> askNickname(null));

        HBox corner = new HBox(10, playerName, button);
        corner.setAlignment(Pos.CENTER_RIGHT);
        corner.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        return corner;
    }

    private void refreshPlayerName() {
        playerName.setText(Settings.hasNickname() ? Settings.nickname() : "Imposta il tuo nome");
    }

    // prima di giocare serve un nome: se manca lo si chiede e poi si prosegue con l'azione scelta
    private void withNickname(Runnable action) {
        if (Settings.hasNickname()) {
            action.run();
        } else {
            askNickname(action);
        }
    }

    // ---------------------------------------------------------------- finestre

    private void askNickname(Runnable then) {
        TextField field = new TextField(Settings.nickname());
        field.setPromptText("Il tuo nome");
        Label error = errorLabel();
        Button save = new Button("Salva");
        save.setDefaultButton(true);
        save.setOnAction(e -> {
            String name = field.getText().trim();
            if (name.isEmpty()) {
                error.setText("Scrivi un nome");
            } else if (name.length() > 20) {
                error.setText("Massimo 20 caratteri");
            } else {
                Settings.setNickname(name);
                refreshPlayerName();
                closeDialog();
                if (then != null) {
                    then.run();
                }
            }
        });
        openDialog("Il tuo nome", "Gli altri giocatori ti vedranno con questo nome.", field, error, buttons(save));
        field.requestFocus();
    }

    private void askRoomName() {
        TextField field = new TextField(Settings.nickname() + "'s pond");
        field.setPromptText("Nome della stanza");
        Label error = errorLabel();
        Button create = new Button("Crea e entra");
        create.setDefaultButton(true);
        create.setOnAction(e -> {
            String name = field.getText().trim();
            if (name.isEmpty()) {
                error.setText("Scrivi un nome per la stanza");
            } else {
                closeDialog();
                onCreateRoom.accept(name);
            }
        });
        openDialog("Crea Stanza", "Gli altri potranno entrare dalla lista delle stanze.", field, error, buttons(create));
        field.requestFocus();
        field.selectAll();
    }

    private void openSettings() {
        TextField host = new TextField(Settings.host());
        host.setPromptText(Settings.DEFAULT_HOST);
        TextField port = new TextField(String.valueOf(Settings.port()));
        port.setPrefColumnCount(6);
        CheckBox skip = new CheckBox("Salta l'animazione iniziale");
        skip.setSelected(Settings.skipIntro());
        Label error = errorLabel();

        Button save = new Button("Salva");
        save.setDefaultButton(true);
        save.setOnAction(e -> {
            String hostText = host.getText().trim().isEmpty() ? Settings.DEFAULT_HOST : host.getText().trim();
            int portNumber;
            try {
                portNumber = Integer.parseInt(port.getText().trim());
            } catch (NumberFormatException ex) {
                error.setText("La porta deve essere un numero");
                return;
            }
            boolean serverChanged = !hostText.equals(Settings.host()) || portNumber != Settings.port();
            Settings.setServer(hostText, portNumber);
            Settings.setSkipIntro(skip.isSelected());
            closeDialog();
            if (serverChanged) {
                onServerChanged.run();
            }
        });

        Label serverLabel = new Label("Server");
        serverLabel.getStyleClass().add("panel-title");
        HBox server = new HBox(8, host, new Label(":"), port);
        server.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(host, Priority.ALWAYS);
        openDialog("Impostazioni", null, serverLabel, server, skip, error, buttons(save));
    }

    private HBox buttons(Button main) {
        Button cancel = new Button("Annulla");
        for (Button button : new Button[] {cancel, main}) {
            button.getStyleClass().add("wood-button");
            button.setMinWidth(Region.USE_PREF_SIZE);
            button.setPrefWidth(Math.max(110, button.prefWidth(-1)));
        }
        cancel.setCancelButton(true);
        cancel.setOnAction(e -> closeDialog());
        HBox row = new HBox(8, cancel, main);
        row.setAlignment(Pos.CENTER_RIGHT);
        return row;
    }

    private static Label errorLabel() {
        Label error = new Label();
        error.setStyle("-fx-text-fill: #c8341f;");
        return error;
    }

    private void openDialog(String title, String subtitle, Node... content) {
        Region dim = new Region();
        dim.setStyle("-fx-background-color: rgba(10, 20, 20, 0.5);");
        dim.setOnMouseClicked(e -> closeDialog());

        Label heading = new Label(title);
        heading.setFont(Theme.title(22));
        VBox panel = new VBox(10, heading);
        if (subtitle != null) {
            Label text = new Label(subtitle);
            text.getStyleClass().add("muted");
            text.setWrapText(true);
            panel.getChildren().add(text);
        }
        panel.getChildren().addAll(content);
        panel.getStyleClass().add("koi-panel");
        panel.setPrefWidth(420);

        // loto con la sua foglia appoggiato sull'angolo in alto a sinistra del pannello
        ImageView pad = decoration("lilypad.png", 64);
        ImageView lotus = decoration("lotus.png", 52);
        StackPane.setAlignment(pad, Pos.TOP_LEFT);
        StackPane.setAlignment(lotus, Pos.TOP_LEFT);
        pad.setTranslateX(-26);
        pad.setTranslateY(-14);
        lotus.setTranslateX(-18);
        lotus.setTranslateY(-30);
        StackPane framed = new StackPane(panel, pad, lotus);
        framed.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        dialogLayer.getChildren().setAll(dim, framed);
        dialogLayer.setVisible(true);
        FadeTransition fade = new FadeTransition(Duration.millis(180), dialogLayer);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private static ImageView decoration(String file, double width) {
        ImageView view = new ImageView(new Image(Objects.requireNonNull(
                TitleScreen.class.getResourceAsStream("/assets/ui/" + file), "missing " + file)));
        view.setPreserveRatio(true);
        view.setFitWidth(width);
        view.setSmooth(true);
        view.setMouseTransparent(true);
        return view;
    }

    private void closeDialog() {
        dialogLayer.setVisible(false);
        dialogLayer.getChildren().clear();
    }

    // ---------------------------------------------------------------- messaggi

    // messaggi ed errori del server mentre si e' nel menu: compaiono in basso e svaniscono da soli
    void showToast(String text) {
        toast.setText(text);
        if (toastFade != null) {
            toastFade.stop();
        }
        toast.setOpacity(1);
        FadeTransition out = new FadeTransition(Duration.seconds(1), toast);
        out.setToValue(0);
        toastFade = new SequentialTransition(new PauseTransition(Duration.seconds(4)), out);
        toastFade.play();
    }
}
