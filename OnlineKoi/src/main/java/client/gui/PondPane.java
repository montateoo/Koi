package client.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Text;
import javafx.util.Duration;
import network.CellView;
import network.HexCoord;

// disegna uno stagno: esagoni a punta piatta, coordinate assiali (q, r) come in HexPosition.
// Animazioni: increspature sull'acqua, carpe che nuotano, lanterne che tremolano, tessere nuove che "sbocciano".
public class PondPane extends StackPane {

    private static final double SQRT3 = Math.sqrt(3);

    // stesso ordine di HexPosition.DIRECTIONS: 0 = SE, 1 = NE, 2 = N, 3 = NW, 4 = SW, 5 = S
    static final int[] DQ = {1, 1, 0, -1, -1, 0};
    static final int[] DR = {0, -1, -1, 0, 1, 1};

    private final double size;
    private final boolean animated;

    private final Group drawing = new Group();
    private final Group tiles = new Group();
    private final Group tops = new Group();
    private final Group effects = new Group();
    private final Group spots = new Group();
    private final Group ghost = new Group();

    private final List<Animation> running = new ArrayList<>();
    private final List<double[]> waterCentres = new ArrayList<>();
    private final Random random = new Random();

    // per animare solo cio' che e' appena comparso
    private Set<String> previousBases = null;
    private Set<String> previousTops = null;

    private HexCoord hovered = null;     // esagono evidenziato sotto il mouse, per l'anteprima
    private Consumer<HexCoord> onHexClicked = coord -> { };
    private Function<HexCoord, List<HexCoord>> ghostShape = coord -> List.of(coord);
    private IntConsumer onKoiClicked = id -> { };
    private Set<Integer> movableKoi = Set.of();   // carpe che si possono cliccare per spostarle
    private int selectedKoi = -1;                 // carpa scelta, in attesa della destinazione

    public PondPane(double size, boolean animated) {
        this.size = size;
        this.animated = animated;
        effects.setMouseTransparent(true);
        ghost.setMouseTransparent(true);
        drawing.getChildren().addAll(tiles, effects, tops, spots, ghost);
        getChildren().add(drawing);

        if (animated) {
            Timeline ripples = new Timeline(new KeyFrame(Duration.seconds(0.9), e -> spawnRipple()));
            ripples.setCycleCount(Animation.INDEFINITE);
            ripples.play();
        }
    }

    private boolean autoFit = false;

    // zoom con la rotella e spostamento trascinando, sopra l'adattamento automatico
    private double userZoom = 1;
    private double panX = 0;
    private double panY = 0;
    private double dragStartX;
    private double dragStartY;
    private double panStartX;
    private double panStartY;

    // se attivo, lo stagno si ingrandisce o rimpicciolisce per riempire lo spazio disponibile
    public void setAutoFit(boolean autoFit) {
        this.autoFit = autoFit;
        requestLayout();
    }

    // rotella = zoom verso il puntatore, trascinare = spostare, doppio clic = vista iniziale
    public void enableZoomAndPan() {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);

        setOnScroll(e -> {
            if (e.getDeltaY() == 0) {
                return;
            }
            double zoom = Math.max(0.4, Math.min(4, userZoom * (e.getDeltaY() > 0 ? 1.12 : 1 / 1.12)));
            double ratio = zoom / userZoom;
            // il punto sotto il puntatore resta fermo: pan' = M - ratio * (M - pan), con M relativo al centro
            double mx = e.getX() - getWidth() / 2;
            double my = e.getY() - getHeight() / 2;
            panX = mx - ratio * (mx - panX);
            panY = my - ratio * (my - panY);
            userZoom = zoom;
            requestLayout();
            e.consume();
        });
        setOnMousePressed(e -> {
            dragStartX = e.getX();
            dragStartY = e.getY();
            panStartX = panX;
            panStartY = panY;
        });
        setOnMouseDragged(e -> {
            setCursor(Cursor.CLOSED_HAND);
            panX = panStartX + e.getX() - dragStartX;
            panY = panStartY + e.getY() - dragStartY;
            drawing.setTranslateX(panX);
            drawing.setTranslateY(panY);
        });
        setOnMouseReleased(e -> setCursor(Cursor.DEFAULT));
        setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && e.isStillSincePress()) {
                resetView();
            }
        });
    }

    public void resetView() {
        userZoom = 1;
        panX = 0;
        panY = 0;
        requestLayout();
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        drawing.setTranslateX(panX);
        drawing.setTranslateY(panY);
        if (!autoFit || tiles.getChildren().isEmpty()) {
            drawing.setScaleX(userZoom);
            drawing.setScaleY(userZoom);
            return;
        }
        // solo tessere e posizioni valide: increspature e anteprima non devono far "respirare" la scala
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Group layer : List.of(tiles, spots)) {
            if (layer.getChildren().isEmpty()) {
                continue;
            }
            javafx.geometry.Bounds b = layer.getBoundsInParent();
            minX = Math.min(minX, b.getMinX());
            minY = Math.min(minY, b.getMinY());
            maxX = Math.max(maxX, b.getMaxX());
            maxY = Math.max(maxY, b.getMaxY());
        }
        double width = maxX - minX;
        double height = maxY - minY;
        if (width <= 0 || height <= 0 || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        double scale = Math.min((getWidth() - 60) / width, (getHeight() - 60) / height);
        scale = Math.max(0.45, Math.min(1.7, scale));
        drawing.setScaleX(scale * userZoom);
        drawing.setScaleY(scale * userZoom);
    }

    // chiamato quando il giocatore clicca un esagono evidenziato
    public void setOnHexClicked(Consumer<HexCoord> handler) {
        this.onHexClicked = handler;
    }

    // chiamato quando il giocatore clicca una carpa che puo' spostare
    public void setOnKoiClicked(IntConsumer handler) {
        this.onKoiClicked = handler;
    }

    // da chiamare prima di show: quali carpe sono cliccabili e quale e' selezionata (-1 nessuna)
    public void setMovableKoi(Set<Integer> ids, int selected) {
        this.movableKoi = Set.copyOf(ids);
        this.selectedKoi = selected;
    }

    // dato l'esagono sotto il mouse, quali esagoni occuperebbe la tessera selezionata (anteprima)
    public void setGhostShape(Function<HexCoord, List<HexCoord>> shape) {
        this.ghostShape = shape;
    }

    public void show(List<CellView> cells, List<HexCoord> highlighted) {
        for (Animation animation : running) {
            animation.stop();
        }
        running.clear();
        tiles.getChildren().clear();
        tops.getChildren().clear();
        spots.getChildren().clear();
        ghost.getChildren().clear();
        hovered = null;
        waterCentres.clear();

        Map<String, CellView> byPosition = new HashMap<>();
        Set<String> bases = new HashSet<>();
        Set<String> topKeys = new HashSet<>();
        for (CellView cell : cells) {
            byPosition.put(key(cell.q(), cell.r()), cell);
            bases.add(key(cell.q(), cell.r()) + cell.base());
            if (cell.top() != null) {
                topKeys.add(key(cell.q(), cell.r()) + cell.top());
            }
        }

        for (CellView cell : cells) {
            Group node = drawCell(cell, byPosition);
            tiles.getChildren().add(node);
            if (previousBases != null && !previousBases.contains(key(cell.q(), cell.r()) + cell.base())) {
                popIn(node);
            }
        }
        drawTops(cells, topKeys);
        for (HexCoord coord : highlighted) {
            drawSpot(coord);
        }

        previousBases = bases;
        previousTops = topKeys;
        requestLayout();
    }

    // ---------------------------------------------------------------- tessere

    private Group drawCell(CellView cell, Map<String, CellView> byPosition) {
        double cx = centerX(cell.q(), cell.r());
        double cy = centerY(cell.q(), cell.r());
        Group group = new Group();

        Polygon hex = KoiArt.hexagon(cx, cy, size);
        // aspetto della base: i pezzi tipo sponda sono pietra; il salice e' prato (senza lati azzurri)
        // oppure acqua con le foglie, come il fogliame
        boolean willow = cell.base().equals("THREE");
        String look;
        if (willow) {
            look = cell.blueSides().contains(true) ? "LEAF" : "GRASS";
        } else if (cell.base().equals("BORDER") || ObjectiveArt.isPondPiece(cell.base())) {
            look = "BORDER";
        } else {
            look = cell.base();
        }
        switch (look) {
            case "BORDER":
                hex.setFill(KoiArt.stoneFill(cx, cy, size));
                break;
            case "GRASS":
                hex.setFill(KoiArt.grassFill(cx, cy, size));
                break;
            case "WATERSOURCE":
                hex.setFill(KoiArt.waterFill(cx, cy, size, true));
                waterCentres.add(new double[] {cx, cy});
                break;
            default:
                hex.setFill(KoiArt.waterFill(cx, cy, size, false));
                waterCentres.add(new double[] {cx, cy});
        }
        Tooltip.install(hex, new Tooltip("(" + cell.q() + "," + cell.r() + ")  " + PlayerPanel.readable(cell.base()).toLowerCase()
                + (cell.top() == null ? "" : " + " + PlayerPanel.readable(cell.top()).toLowerCase())));
        group.getChildren().add(hex);

        switch (look) {
            case "GRASS":
                group.getChildren().add(KoiArt.grassTufts(cx, cy, size, Math.abs(cell.q() * 5 + cell.r() * 3)));
                break;
            case "LEAF":
                group.getChildren().add(KoiArt.floatingLeaves(cx, cy, size, Math.abs(cell.q() * 7 + cell.r() * 3)));
                break;
            case "WATERSOURCE":
                for (int i = 1; i <= 2; i++) {
                    Circle ring = new Circle(cx, cy, size * 0.22 * i, null);
                    ring.setStroke(Color.color(1, 1, 1, 0.45));
                    group.getChildren().add(ring);
                }
                break;
            case "BORDER":
                break;
            default:
                if (size >= 20) {
                    group.getChildren().add(KoiArt.waves(cx, cy, size));
                }
        }

        // lati: azzurri o di roccia; tra esagoni della stessa tessera solo una linea sottile
        for (int corner = 0; corner < 6; corner++) {
            int direction = (6 - corner) % 6;      // il lato tra l'angolo k e k+1 guarda nella direzione (6-k)%6
            CellView neighbour = byPosition.get(key(cell.q() + DQ[direction], cell.r() + DR[direction]));
            double x1 = cornerX(cx, corner), y1 = cornerY(cy, corner);
            double x2 = cornerX(cx, corner + 1), y2 = cornerY(cy, corner + 1);
            Line side = new Line(x1, y1, x2, y2);
            side.setStrokeLineCap(StrokeLineCap.ROUND);
            if (neighbour != null && neighbour.tileId() == cell.tileId()) {
                side.setStroke(Color.color(1, 1, 1, 0.3));
                side.setStrokeWidth(1);
            } else if (cell.blueSides().get(direction)) {
                side.setStroke(Theme.TEAL_DEEP.deriveColor(0, 1, 1, 0.75));
                side.setStrokeWidth(Math.max(1, size * 0.05));
            } else {
                side.setStroke(Theme.WOOD_DARK);
                side.setStrokeWidth(Math.max(2, size * 0.13));
                group.getChildren().addAll(rocks(cx, cy, x1, y1, x2, y2));
            }
            group.getChildren().add(side);
        }
        return group;
    }

    // ciottoli lungo un lato di roccia della sponda
    private List<Node> rocks(double cx, double cy, double x1, double y1, double x2, double y2) {
        List<Node> rocks = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            double t = 0.2 + i * 0.3;
            double x = x1 + (x2 - x1) * t;
            double y = y1 + (y2 - y1) * t;
            // spostati un po' verso l'interno dell'esagono
            x += (cx - x) * 0.18;
            y += (cy - y) * 0.18;
            Ellipse rock = new Ellipse(x, y, size * 0.13, size * 0.09);
            rock.setFill(i % 2 == 0 ? Theme.STONE_DARK : Color.web("#a69c88"));
            rock.setStroke(Color.color(0.3, 0.25, 0.2, 0.5));
            rocks.add(rock);
        }
        return rocks;
    }

    // ---------------------------------------------------------------- cio' che sta sopra

    private void drawTops(List<CellView> cells, Set<String> topKeys) {
        Map<Integer, List<CellView>> koiById = new LinkedHashMap<>();
        Map<Integer, List<CellView>> objectivesById = new LinkedHashMap<>();
        Map<Integer, List<CellView>> piecesById = new LinkedHashMap<>();   // statua, ponticello... sono la base stessa
        for (CellView cell : cells) {
            if (ObjectiveArt.isPondPiece(cell.base())) {
                piecesById.computeIfAbsent(cell.tileId(), id -> new ArrayList<>()).add(cell);
            }
            if (cell.top() == null) {
                continue;
            }
            double cx = centerX(cell.q(), cell.r());
            double cy = centerY(cell.q(), cell.r());
            boolean fresh = previousTops != null && !previousTops.contains(key(cell.q(), cell.r()) + cell.top());
            switch (cell.top()) {
                case "LILY": {
                    Group lily = KoiArt.lily(size * 0.62);
                    lily.setLayoutX(cx);
                    lily.setLayoutY(cy);
                    lily.setRotate((cell.q() * 37 + cell.r() * 61) % 360);
                    tops.getChildren().add(lily);
                    if (fresh) {
                        popIn(lily);
                    }
                    break;
                }
                case "LANTERN": {
                    Group lantern = KoiArt.lantern(size * 1.35);
                    lantern.setLayoutX(cx);
                    lantern.setLayoutY(cy - size * 0.05);
                    tops.getChildren().add(lantern);
                    flicker(lantern.lookup("#light"));
                    if (fresh) {
                        popIn(lantern);
                    }
                    break;
                }
                default:
                    if (cell.top().endsWith("KOI")) {
                        koiById.computeIfAbsent(cell.topId(), id -> new ArrayList<>()).add(cell);
                    } else {
                        objectivesById.computeIfAbsent(cell.topId(), id -> new ArrayList<>()).add(cell);
                    }
            }
        }

        for (Map.Entry<Integer, List<CellView>> koi : koiById.entrySet()) {
            drawKoi(koi.getKey(), koi.getValue());
        }
        for (List<CellView> objective : objectivesById.values()) {
            boolean fresh = false;
            for (CellView cell : objective) {
                fresh |= previousTops != null && !previousTops.contains(key(cell.q(), cell.r()) + cell.top());
            }
            drawObjective(objective, objective.get(0).top(), objective.get(0).topRotation(), true, fresh);
        }
        for (List<CellView> piece : piecesById.values()) {
            boolean fresh = false;
            for (CellView cell : piece) {
                fresh |= previousBases != null && !previousBases.contains(key(cell.q(), cell.r()) + cell.base());
            }
            // il salice sta sul prato, non al centro del pezzo (coprirebbe i sassi e l'acqua)
            List<CellView> under = piece;
            if (piece.get(0).base().equals("THREE")) {
                under = new ArrayList<>();
                for (CellView cell : piece) {
                    if (!cell.blueSides().contains(true)) {
                        under.add(cell);
                    }
                }
            }
            drawObjective(under, piece.get(0).base(), 0, false, fresh);
        }
    }

    // tessera obiettivo: il disegno sul baricentro dei suoi esagoni, girato come la tessera;
    // per quelle sopra l'acqua anche un'impronta dorata, cosi' si vede quali esagoni coprono
    private void drawObjective(List<CellView> cells, String type, int rotation, boolean veiled, boolean fresh) {
        double sumX = 0, sumY = 0;
        for (CellView cell : cells) {
            double x = centerX(cell.q(), cell.r());
            double y = centerY(cell.q(), cell.r());
            sumX += x;
            sumY += y;
            if (veiled) {
                Polygon veil = KoiArt.hexagon(x, y, size * 0.9);
                veil.setFill(Color.color(0.83, 0.63, 0.23, 0.18));
                veil.setStroke(Color.color(0.83, 0.63, 0.23, 0.7));
                veil.setStrokeWidth(1.2);
                tops.getChildren().add(veil);
            }
        }
        Group art = ObjectiveArt.objective(type, size, cells.size());
        if (ObjectiveArt.hasShape(type)) {
            ObjectiveArt.orient(art, rotation);
        }
        Group holder = new Group(art);   // il contenitore si sposta, il disegno dentro ruota attorno a (0, 0)
        holder.setLayoutX(sumX / cells.size());
        holder.setLayoutY(sumY / cells.size());
        Tooltip.install(holder, new Tooltip(PlayerPanel.readable(type)));
        tops.getChildren().add(holder);
        if (fresh) {
            popIn(holder);
        }
    }

    // una carpa sola anche se copre piu' esagoni: al centro delle sue celle, orientata secondo la rotazione
    private void drawKoi(int id, List<CellView> cells) {
        double sumX = 0, sumY = 0;
        for (CellView cell : cells) {
            sumX += centerX(cell.q(), cell.r());
            sumY += centerY(cell.q(), cell.r());
        }
        double cx = sumX / cells.size();
        double cy = sumY / cells.size();
        String type = cells.get(0).top();

        double angle;
        if (cells.size() == 2) {
            angle = Math.toDegrees(Math.atan2(
                    centerY(cells.get(1).q(), cells.get(1).r()) - centerY(cells.get(0).q(), cells.get(0).r()),
                    centerX(cells.get(1).q(), cells.get(1).r()) - centerX(cells.get(0).q(), cells.get(0).r())));
        } else {
            angle = 30 - 60 * cells.get(0).topRotation();   // direzione d sullo schermo: 30 - 60*d gradi
        }
        double length = size * (type.equals("SMALLKOI") ? 1.35 : type.equals("MEDIUMKOI") ? 2.5 : 2.9);

        Group fish = KoiArt.koi(length, id);
        Group holder = new Group(fish);      // il contenitore orienta, il pesce dentro "nuota"
        holder.setLayoutX(cx);
        holder.setLayoutY(cy);
        holder.setRotate(angle);
        tops.getChildren().add(holder);
        if (movableKoi.contains(id)) {
            holder.setCursor(Cursor.HAND);
            holder.setOnMouseClicked(e -> {
                if (e.isStillSincePress()) {
                    onKoiClicked.accept(id);
                }
                e.consume();
            });
            if (id == selectedKoi) {
                holder.setEffect(new DropShadow(size * 0.5, Theme.GOLD));
            }
        }

        if (animated) {
            double delay = random.nextDouble() * 1.5;
            Timeline swim = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(fish.rotateProperty(), -6, Interpolator.EASE_BOTH),
                            new KeyValue(fish.translateXProperty(), -size * 0.04, Interpolator.EASE_BOTH)),
                    new KeyFrame(Duration.seconds(1.6),
                            new KeyValue(fish.rotateProperty(), 6, Interpolator.EASE_BOTH),
                            new KeyValue(fish.translateXProperty(), size * 0.04, Interpolator.EASE_BOTH)));
            swim.setAutoReverse(true);
            swim.setCycleCount(Animation.INDEFINITE);
            swim.setDelay(Duration.seconds(delay));
            swim.play();
            running.add(swim);
        }
        boolean fresh = false;
        for (CellView cell : cells) {
            fresh |= previousTops != null && !previousTops.contains(key(cell.q(), cell.r()) + cell.top());
        }
        if (fresh) {
            popIn(holder);
        }
    }

    // ---------------------------------------------------------------- posizioni valide e anteprima

    private void drawSpot(HexCoord coord) {
        double cx = centerX(coord.q(), coord.r());
        double cy = centerY(coord.q(), coord.r());
        Polygon spot = KoiArt.hexagon(cx, cy, size * 0.92);
        spot.setFill(Color.color(1, 0.94, 0.7, 0.4));
        spot.setStroke(Theme.GOLD);
        spot.setStrokeWidth(2.5);
        spot.getStrokeDashArray().addAll(7.0, 5.0);
        spot.setStyle("-fx-cursor: hand;");
        spot.setOnMouseClicked(e -> {
            if (e.isStillSincePress()) {       // un trascinamento per spostare lo stagno non deve piazzare nulla
                onHexClicked.accept(coord);
            }
            e.consume();
        });
        spot.setOnMouseEntered(e -> showGhost(coord));
        spot.setOnMouseExited(e -> {
            hovered = null;
            ghost.getChildren().clear();
        });

        Text label = new Text(cx - 13, cy + 4, coord.q() + "," + coord.r());
        label.setFont(Theme.serif(Math.max(9, size * 0.3)));
        label.setFill(Theme.INK_SOFT);
        label.setMouseTransparent(true);
        spots.getChildren().addAll(spot, label);

        if (animated) {
            FadeTransition pulse = new FadeTransition(Duration.seconds(0.9), spot);
            pulse.setFromValue(0.55);
            pulse.setToValue(1);
            pulse.setAutoReverse(true);
            pulse.setCycleCount(Animation.INDEFINITE);
            pulse.play();
            running.add(pulse);
        }
    }

    // ridisegna l'anteprima, ad esempio dopo un cambio di rotazione con il mouse ancora sopra
    public void refreshGhost() {
        if (hovered != null) {
            showGhost(hovered);
        }
    }

    private void showGhost(HexCoord coord) {
        hovered = coord;
        ghost.getChildren().clear();
        for (HexCoord part : ghostShape.apply(coord)) {
            Polygon hex = KoiArt.hexagon(centerX(part.q(), part.r()), centerY(part.q(), part.r()), size * 0.95);
            hex.setFill(Color.color(1, 1, 1, 0.55));
            hex.setStroke(Theme.VERMILION);
            hex.setStrokeWidth(2.5);
            ghost.getChildren().add(hex);
        }
    }

    // ---------------------------------------------------------------- animazioni

    private void popIn(Node node) {
        if (!animated) {
            return;
        }
        node.setScaleX(0.2);
        node.setScaleY(0.2);
        node.setOpacity(0);
        // cresce un po' oltre la misura giusta e poi si assesta, come un fiore che sboccia
        ScaleTransition grow = new ScaleTransition(Duration.millis(320), node);
        grow.setToX(1.12);
        grow.setToY(1.12);
        grow.setInterpolator(Interpolator.EASE_OUT);
        ScaleTransition settle = new ScaleTransition(Duration.millis(160), node);
        settle.setToX(1);
        settle.setToY(1);
        settle.setInterpolator(Interpolator.EASE_IN);
        FadeTransition fade = new FadeTransition(Duration.millis(300), node);
        fade.setToValue(1);
        new ParallelTransition(new SequentialTransition(grow, settle), fade).play();
    }

    private void flicker(Node light) {
        if (!animated || light == null || !(light.getEffect() instanceof DropShadow)) {
            return;
        }
        DropShadow glow = (DropShadow) light.getEffect();
        Timeline flicker = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(glow.radiusProperty(), size * 0.25)),
                new KeyFrame(Duration.seconds(0.7 + random.nextDouble() * 0.6), new KeyValue(glow.radiusProperty(), size * 0.6)));
        flicker.setAutoReverse(true);
        flicker.setCycleCount(Animation.INDEFINITE);
        flicker.play();
        running.add(flicker);
    }

    // un'increspatura che si allarga e svanisce su un esagono d'acqua a caso
    private void spawnRipple() {
        if (waterCentres.isEmpty() || getScene() == null) {
            return;
        }
        double[] centre = waterCentres.get(random.nextInt(waterCentres.size()));
        double x = centre[0] + (random.nextDouble() - 0.5) * size * 0.6;
        double y = centre[1] + (random.nextDouble() - 0.5) * size * 0.6;
        Circle ring = new Circle(x, y, size * 0.08, null);
        ring.setStroke(Color.color(1, 1, 1, 0.8));
        ring.setStrokeWidth(1.5);
        effects.getChildren().add(ring);
        Timeline grow = new Timeline(
                new KeyFrame(Duration.seconds(1.8),
                        new KeyValue(ring.radiusProperty(), size * 0.55, Interpolator.EASE_OUT),
                        new KeyValue(ring.opacityProperty(), 0)));
        grow.setOnFinished(e -> effects.getChildren().remove(ring));
        grow.play();
    }

    // ---------------------------------------------------------------- geometria

    private static String key(int q, int r) {
        return q + "," + r;
    }

    private double centerX(int q, int r) {
        return size * 1.5 * q;
    }

    private double centerY(int q, int r) {
        return size * SQRT3 * (r + q / 2.0);
    }

    private double cornerX(double cx, int corner) {
        return cx + size * Math.cos(Math.toRadians(60 * corner));
    }

    private double cornerY(double cy, int corner) {
        return cy + size * Math.sin(Math.toRadians(60 * corner));
    }
}
