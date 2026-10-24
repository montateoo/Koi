package client.gui;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;

// disegni originali in stile giapponese, fatti solo con forme JavaFX (nessuna immagine esterna)
final class KoiArt {

    private KoiArt() {
    }

    // ---------------------------------------------------------------- esagoni

    // esagono a punta piatta centrato in (cx, cy)
    static Polygon hexagon(double cx, double cy, double size) {
        Polygon hex = new Polygon();
        for (int corner = 0; corner < 6; corner++) {
            hex.getPoints().addAll(
                    cx + size * Math.cos(Math.toRadians(60 * corner)),
                    cy + size * Math.sin(Math.toRadians(60 * corner)));
        }
        return hex;
    }

    static Paint waterFill(double cx, double cy, double size, boolean source) {
        Color light = source ? Color.web("#a9e2ea") : Theme.TEAL_LIGHT;
        Color deep = source ? Color.web("#3fa6b6") : Theme.TEAL;
        return new RadialGradient(0, 0, cx - size * 0.35, cy - size * 0.35, size * 1.6, false, CycleMethod.NO_CYCLE,
                new Stop(0, light), new Stop(1, deep));
    }

    // prato del salice piangente
    static Paint grassFill(double cx, double cy, double size) {
        return new RadialGradient(0, 0, cx - size * 0.3, cy - size * 0.3, size * 1.5, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#a6d06a")), new Stop(1, Color.web("#5e8f2f")));
    }

    // ciuffi d'erba e qualche fiorellino giallo, sparsi in modo diverso per ogni esagono
    static Group grassTufts(double cx, double cy, double size, int seed) {
        Group tufts = new Group();
        double[][] spots = {{-0.4, -0.25}, {0.3, -0.4}, {0.4, 0.2}, {-0.2, 0.4}, {0.0, -0.05}};
        for (int i = 0; i < spots.length; i++) {
            double x = cx + spots[(i + seed) % spots.length][0] * size;
            double y = cy + spots[(i + seed) % spots.length][1] * size;
            if (i % 2 == 0) {
                Circle flower = new Circle(x, y, Math.max(1, size * 0.05), Color.web("#f3d04a"));
                tufts.getChildren().add(flower);
            } else {
                for (int blade = -1; blade <= 1; blade++) {
                    Line line = new Line(x, y, x + blade * size * 0.06, y - size * 0.12);
                    line.setStroke(Color.web("#3f6e1f"));
                    line.setStrokeWidth(Math.max(1, size * 0.03));
                    tufts.getChildren().add(line);
                }
            }
        }
        return tufts;
    }

    static Paint stoneFill(double cx, double cy, double size) {
        return new RadialGradient(0, 0, cx - size * 0.3, cy - size * 0.3, size * 1.5, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#e3d9c3")), new Stop(1, Color.web("#b3a68c")));
    }

    // due piccole onde bianche, come nei disegni delle tessere Acqua
    static Group waves(double cx, double cy, double size) {
        Group waves = new Group();
        for (int i = 0; i < 2; i++) {
            Arc arc = new Arc(cx - size * 0.28 + i * size * 0.42, cy + size * 0.18 - i * size * 0.36,
                    size * 0.17, size * 0.09, 0, 180);
            arc.setType(ArcType.OPEN);
            arc.setFill(null);
            arc.setStroke(Color.color(1, 1, 1, 0.6));
            arc.setStrokeWidth(Math.max(1, size * 0.045));
            waves.getChildren().add(arc);
        }
        return waves;
    }

    // foglioline gialle e verdi che galleggiano, come sulla tessera Fogliame
    static Group floatingLeaves(double cx, double cy, double size, int seed) {
        Group leaves = new Group();
        double[][] spots = {{-0.35, -0.2}, {0.25, -0.35}, {0.3, 0.25}, {-0.2, 0.35}, {0.0, 0.0}};
        Color[] colors = {Color.web("#e3c53c"), Color.web("#b9cf4a"), Color.web("#d9a52c"), Color.web("#9fc24a")};
        for (int i = 0; i < spots.length; i++) {
            SVGPath leaf = new SVGPath();
            leaf.setContent("M0 0 C3 -4 9 -4 12 0 C9 4 3 4 0 0 Z");
            leaf.setFill(colors[(i + seed) % colors.length]);
            leaf.setStroke(Color.color(0.3, 0.35, 0.1, 0.5));
            double scale = size / 34.0 * 1.15;
            leaf.setScaleX(scale);
            leaf.setScaleY(scale);
            leaf.setLayoutX(cx + spots[i][0] * size - 6);
            leaf.setLayoutY(cy + spots[i][1] * size);
            leaf.setRotate((seed * 47 + i * 73) % 360);
            leaves.getChildren().add(leaf);
        }
        return leaves;
    }

    // ---------------------------------------------------------------- carpa

    // una carpa che guarda verso destra, centrata in (0, 0); palette 0 kohaku, 1 showa, 2 yamabuki
    static Group koi(double length, int palette) {
        Color body;
        Color[] patches;
        switch (Math.floorMod(palette, 3)) {
            case 1:
                body = Color.web("#fffaf2");
                patches = new Color[] {Color.web("#e8452c"), Theme.INK, Color.web("#e8452c")};
                break;
            case 2:
                body = Color.web("#f6cc4a");
                patches = new Color[] {Color.web("#e9a21e"), Color.web("#fbe08a"), Color.web("#e9a21e")};
                break;
            default:
                body = Color.web("#fffaf2");
                patches = new Color[] {Color.web("#e8452c"), Color.web("#e8452c"), Color.web("#f06a3a")};
        }

        Group fish = new Group();

        // coda biforcuta e pinne, un po' trasparenti come veli
        Polygon tail = new Polygon(
                -length * 0.26, 0,
                -length * 0.52, -length * 0.17,
                -length * 0.42, 0,
                -length * 0.52, length * 0.17);
        tail.setFill(body.deriveColor(0, 1, 1, 0.85));
        tail.setStroke(Color.color(0.4, 0.2, 0.1, 0.35));
        Ellipse finTop = new Ellipse(length * 0.08, -length * 0.13, length * 0.1, length * 0.045);
        finTop.setRotate(-35);
        Ellipse finBottom = new Ellipse(length * 0.08, length * 0.13, length * 0.1, length * 0.045);
        finBottom.setRotate(35);
        for (Ellipse fin : new Ellipse[] {finTop, finBottom}) {
            fin.setFill(body.deriveColor(0, 1, 1, 0.75));
            fin.setStroke(Color.color(0.4, 0.2, 0.1, 0.3));
        }

        Ellipse bodyShape = new Ellipse(length * 0.04, 0, length * 0.33, length * 0.13);
        bodyShape.setFill(body);
        bodyShape.setStroke(Color.color(0.35, 0.2, 0.1, 0.45));

        // macchie colorate, ritagliate sulla forma del corpo
        Group spots = new Group(
                new Ellipse(length * 0.2, -length * 0.03, length * 0.12, length * 0.09),
                new Ellipse(-length * 0.06, length * 0.04, length * 0.1, length * 0.08),
                new Ellipse(-length * 0.2, -length * 0.03, length * 0.06, length * 0.05));
        for (int i = 0; i < spots.getChildren().size(); i++) {
            ((Ellipse) spots.getChildren().get(i)).setFill(patches[i]);
        }
        spots.setClip(new Ellipse(length * 0.04, 0, length * 0.33, length * 0.13));

        Circle eyeTop = new Circle(length * 0.3, -length * 0.05, Math.max(1, length * 0.018), Theme.INK);
        Circle eyeBottom = new Circle(length * 0.3, length * 0.05, Math.max(1, length * 0.018), Theme.INK);

        fish.getChildren().addAll(tail, finTop, finBottom, bodyShape, spots, eyeTop, eyeBottom);
        fish.setEffect(new DropShadow(length * 0.08, 0, length * 0.03, Color.color(0, 0.2, 0.3, 0.35)));
        return fish;
    }

    // ---------------------------------------------------------------- ninfea

    // tessera Percorso: tre pietre piatte da guado, centrate in (0, 0)
    static Group pathStones(double radius) {
        Group stones = new Group();
        double[][] layout = {{-0.45, 0.28, 0.42}, {0.08, -0.05, 0.5}, {0.5, -0.4, 0.36}};   // x, y, raggio
        for (double[] stone : layout) {
            Ellipse rock = new Ellipse(radius * stone[0], radius * stone[1], radius * stone[2], radius * stone[2] * 0.72);
            rock.setFill(new RadialGradient(0, 0, radius * stone[0] - radius * 0.1, radius * stone[1] - radius * 0.1,
                    radius * stone[2], false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#e6e1d6")), new Stop(1, Color.web("#9b958a"))));
            rock.setStroke(Color.web("#6f6a60"));
            rock.setStrokeWidth(1.2);
            stones.getChildren().add(rock);
        }
        stones.setEffect(new DropShadow(radius * 0.2, Color.color(0, 0.15, 0.2, 0.4)));
        return stones;
    }

    // ninfea su una foglia rotonda con la tacca, centrata in (0, 0)
    static Group lily(double radius) {
        return lily(radius, Color.web("#ef6f9a"), Color.web("#c2185b"));
    }

    // stessa ninfea con i petali sfumati verso "tip" (la Ninfea viola degli obiettivi)
    static Group lily(double radius, Color tip, Color outline) {
        Arc pad = new Arc(0, 0, radius, radius, 25, 310);
        pad.setType(ArcType.ROUND);
        pad.setFill(new RadialGradient(0, 0, 0, 0, radius, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#8cc66b")), new Stop(1, Color.web("#4f8a3a"))));
        pad.setStroke(Color.web("#3d6e2c"));

        Group flower = new Group();
        Paint petalFill = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#fff0f5")), new Stop(1, tip));
        for (int i = 0; i < 8; i++) {
            Ellipse petal = new Ellipse(radius * 0.32, 0, radius * 0.33, radius * 0.13);
            petal.setFill(petalFill);
            petal.setStroke(outline.deriveColor(0, 1, 1, 0.5));
            petal.getTransforms().add(new Rotate(i * 45 + 22, 0, 0));
            flower.getChildren().add(petal);
        }
        Circle heart = new Circle(0, 0, radius * 0.15, Color.web("#ffd54f"));
        heart.setStroke(Color.web("#e0a800"));
        flower.getChildren().add(heart);
        flower.setScaleX(0.9);
        flower.setScaleY(0.9);

        Group lily = new Group(pad, flower);
        lily.setEffect(new DropShadow(radius * 0.25, Color.color(0, 0.2, 0.1, 0.35)));
        return lily;
    }

    // ---------------------------------------------------------------- lanterna

    // lanterna di pietra (toro) alta "height", con il piede in (0, height/2); la finestra accesa ha id "light"
    static Group lantern(double height) {
        double h = height;
        Color stone = Color.web("#9aa0a0");
        Color stoneDark = Color.web("#6f7676");

        Rectangle base = new Rectangle(-h * 0.24, h * 0.36, h * 0.48, h * 0.12);
        Rectangle pillar = new Rectangle(-h * 0.08, h * 0.12, h * 0.16, h * 0.26);
        Rectangle box = new Rectangle(-h * 0.2, -h * 0.12, h * 0.4, h * 0.26);
        Rectangle light = new Rectangle(-h * 0.1, -h * 0.07, h * 0.2, h * 0.16);
        light.setId("light");
        light.setFill(Color.web("#ffd54f"));
        light.setEffect(new DropShadow(h * 0.25, Color.web("#ffca28")));
        Polygon roof = new Polygon(
                -h * 0.36, -h * 0.12,
                h * 0.36, -h * 0.12,
                h * 0.2, -h * 0.3,
                -h * 0.2, -h * 0.3);
        Circle knob = new Circle(0, -h * 0.36, h * 0.07);

        for (Rectangle part : new Rectangle[] {base, pillar, box}) {
            part.setFill(stone);
            part.setStroke(stoneDark);
            part.setArcWidth(h * 0.05);
            part.setArcHeight(h * 0.05);
        }
        roof.setFill(stoneDark);
        knob.setFill(stoneDark);

        return new Group(base, pillar, box, light, roof, knob);
    }

    // ---------------------------------------------------------------- sigilli, monete, gettoni

    // sigillo rosso quadrato con un kanji bianco (o la lettera di riserva se mancano i font giapponesi)
    static StackPane seal(String kanji, String fallback, double size, Color color) {
        Rectangle stamp = new Rectangle(size, size, color);
        stamp.setArcWidth(size * 0.25);
        stamp.setArcHeight(size * 0.25);
        Text sign = new Text(Theme.hasJapanese() ? kanji : fallback);
        sign.setFont(Theme.kanji(size * 0.62));
        sign.setFill(Theme.PAPER_LIGHT);
        StackPane seal = new StackPane(stamp, sign);
        seal.setRotate(-4);
        return seal;
    }

    // moneta d'oro con il foro quadrato
    static Group coin(double radius) {
        Circle disc = new Circle(radius, new RadialGradient(0, 0, -radius * 0.3, -radius * 0.3, radius * 1.6, false,
                CycleMethod.NO_CYCLE, new Stop(0, Color.web("#ffe08a")), new Stop(1, Theme.GOLD)));
        disc.setStroke(Color.web("#a07818"));
        Rectangle hole = new Rectangle(-radius * 0.28, -radius * 0.28, radius * 0.56, radius * 0.56);
        hole.setFill(Theme.PAPER);
        hole.setStroke(Color.web("#a07818"));
        return new Group(disc, hole);
    }

    // segnalino Fortuna: disco vermiglio con il kanji della fortuna
    static StackPane fortune(double radius) {
        Circle disc = new Circle(radius, Theme.VERMILION);
        disc.setStroke(Color.web("#8e1f10"));
        Text sign = new Text(Theme.hasJapanese() ? "福" : "F");
        sign.setFont(Theme.kanji(radius * 1.15));
        sign.setFill(Theme.PAPER_LIGHT);
        return new StackPane(disc, sign);
    }

    // icona di una tessera per il magazzino, centrata in (0, 0)
    static Node tileIcon(String type, double size) {
        Polygon hex = hexagon(0, 0, size);
        hex.setStroke(Theme.WOOD_DARK);
        hex.setStrokeWidth(1.5);
        switch (type) {
            case "LEAF":
                hex.setFill(waterFill(0, 0, size, false));
                return new Group(hex, floatingLeaves(0, 0, size, 1));
            case "BORDER":
                hex.setFill(stoneFill(0, 0, size));
                Group stones = new Group(hex);
                for (int i = 0; i < 5; i++) {
                    Ellipse s = new Ellipse(-size * 0.45 + i * size * 0.22, -size * 0.25 + (i % 2) * size * 0.12,
                            size * 0.14, size * 0.1);
                    s.setFill(Theme.STONE_DARK);
                    stones.getChildren().add(s);
                }
                return stones;
            case "LILY":
                hex.setFill(waterFill(0, 0, size, false));
                return new Group(hex, lily(size * 0.62));
            case "LANTERN":
                hex.setFill(stoneFill(0, 0, size));
                return new Group(hex, lantern(size * 1.25));
            case "SMALLKOI":
            case "MEDIUMKOI":
            case "BIGKOI":
                hex.setFill(waterFill(0, 0, size, false));
                double length = size * (type.equals("SMALLKOI") ? 1.3 : type.equals("MEDIUMKOI") ? 1.6 : 1.85);
                Group fish = koi(length, type.equals("BIGKOI") ? 2 : type.equals("MEDIUMKOI") ? 1 : 0);
                fish.setRotate(-30);
                return new Group(hex, fish);
            default:
                hex.setFill(waterFill(0, 0, size, false));
                return new Group(hex, waves(0, 0, size));
        }
    }
}
