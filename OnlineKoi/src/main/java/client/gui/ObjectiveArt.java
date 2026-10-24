package client.gui;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.QuadCurve;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

// tessere obiettivo disegnate in codice, ispirate alle illustrazioni del regolamento ma originali.
// Tutte le misure sono in "s" = raggio di un esagono. Le tessere con una forma (rami, shishi odoshi,
// carpa centenaria) sono disegnate attorno al baricentro dei loro esagoni a rotazione 0:
// PondPane le mette sul baricentro reale e con orient() applica rotazione e specchiatura.
final class ObjectiveArt {

    private static final double H = Math.sqrt(3) / 2;   // meta' della distanza tra i centri di due esagoni vicini, in s

    private static final Color BARK = Color.web("#5a381d");
    private static final Color BARK_LIGHT = Color.web("#9a6a3c");
    private static final Color STONE = Color.web("#b4ad9c");
    private static final Color STONE_DARK = Color.web("#6f6a5e");
    private static final Color BAMBOO = Color.web("#b3c656");
    private static final Color BAMBOO_DARK = Color.web("#6b7d22");
    private static final Color LACQUER = Color.web("#c8341f");
    private static final Color LACQUER_DARK = Color.web("#7c1c10");
    private static final Color FOLIAGE = Color.web("#7cae3c");
    private static final Color FOLIAGE_DARK = Color.web("#4d7a24");
    private static final Color WATER_LIGHT = Color.web("#d9f4f7");
    private static final Color WATER = Color.web("#5cc2d0");

    private ObjectiveArt() {
    }

    // tessere viste dall'alto che seguono la loro figura di esagoni, quindi ruotano e si specchiano con la tessera.
    // Lo shishi odoshi no: e' un oggetto in piedi, resta dritto sul baricentro del suo triangolo
    static boolean hasShape(String type) {
        switch (type) {
            case "SMALLBRANCH":
            case "MEDIUMBRANCH":
            case "BIGBRANCH":
            case "OLDCARP":
                return true;
            default:
                return false;
        }
    }

    // obiettivi che sono pezzi nuovi di laghetto (piazzati come una sponda), non tessere sopra l'acqua
    static boolean isPondPiece(String base) {
        switch (base) {
            case "STATUE":
            case "BRIDGE":
            case "WATERFALL":
            case "THREE":
                return true;
            default:
                return false;
        }
    }

    // il disegno della tessera, centrato in (0, 0); "cells" scala le tessere senza una forma fissa
    static Group objective(String type, double s, int cells) {
        switch (type) {
            case "SMALLBRANCH":  return smallBranch(s);
            case "MEDIUMBRANCH": return mediumBranch(s);
            case "BIGBRANCH":    return bigBranch(s);
            case "SHISHIODOSHI": return scaled(shishiOdoshi(s), 2);
            case "OLDCARP":      return oldCarp(s);
            case "TURTLE":       return turtle(s);
            case "PURPLELILY":   return KoiArt.lily(s * 0.62, Color.web("#a35bc9"), Color.web("#5b2c6f"));
            case "PATH":         return KoiArt.pathStones(s * 0.8);
            case "THREE":        return scaled(willow(s), cells);
            case "WATERFALL":    return scaled(waterfall(s), cells);
            case "BRIDGE":       return scaled(bridge(s), cells);
            case "STATUE":       return scaled(statue(s), cells);
            default:             return new Group();
        }
    }

    // rotazione 0-5, e 6-11 per la forma specchiata (come in Pond): la specchiatura scambia la direzione d con -d,
    // cioe' riflette attorno alla direzione 0, che sullo schermo sta a +30 gradi
    static void orient(Node art, int rotation) {
        art.getTransforms().add(new Rotate(-60 * (rotation % 6)));
        if (rotation >= 6) {
            art.getTransforms().addAll(new Rotate(30), new Scale(1, -1), new Rotate(-30));
        }
    }

    private static Group scaled(Group art, int cells) {
        double factor = cells <= 1 ? 1 : Math.sqrt(cells) * 1.1;   // pezzi da 3 esagoni: il disegno occupa tutta la tessera
        art.getTransforms().add(new Scale(factor, factor));
        return art;
    }

    // ---------------------------------------------------------------- rami

    // ramo piccolo: tre esagoni in fila verso l'alto, centrato su quello di mezzo
    private static Group smallBranch(double s) {
        Group g = new Group();
        double w = 0.2;
        limb(g, s, w, 0.15, 2.1 * H, -0.55, 1.2, 0.55, -0.45, -0.05, -2.1 * H);
        twig(g, s, -0.2, 1.0, -165, 0.75, w * 0.45, true);
        twig(g, s, 0.22, -0.15, -15, 0.7, w * 0.45, true);
        twig(g, s, 0.05, -1.15, -150, 0.6, w * 0.4, false);
        fork(g, s, -0.05, -2.1 * H, -90, 0.42, w * 0.35);
        return g;
    }

    // ramo medio: un nodo al centro e tre rami a 120 gradi, come un palco di cervo
    private static Group mediumBranch(double s) {
        Group g = new Group();
        double w = 0.22;
        limb(g, s, w, 0, 0.1, 0.18, -0.7, -0.22, -1.3, 0.05, -2 * H - 0.15);
        limb(g, s, w, 0, 0, -0.5, 0.15, -0.95, 0.9, -1.65, H + 0.1);
        limb(g, s, w, 0, 0, 0.5, 0.25, 1.0, 0.65, 1.65, H + 0.1);
        knot(g, s, 0, 0, w);
        fork(g, s, 0.05, -2 * H - 0.15, -90, 0.45, w * 0.35);
        fork(g, s, -1.65, H + 0.1, 155, 0.45, w * 0.35);
        fork(g, s, 1.65, H + 0.1, 25, 0.45, w * 0.35);
        twig(g, s, -0.75, 0.45, -120, 0.55, w * 0.4, true);
        twig(g, s, 0.8, 0.45, -60, 0.5, w * 0.4, true);
        return g;
    }

    // ramo grande: due rami nodosi che si incrociano a X, come la sorgente
    private static Group bigBranch(double s) {
        Group g = new Group();
        double w = 0.25;
        limb(g, s, w, -1.7, H + 0.1, -0.6, 0.7, 0.45, -0.55, 1.7, -H - 0.1);
        limb(g, s, w, -1.7, -H - 0.1, -0.45, -0.35, 0.65, 0.55, 1.7, H + 0.1);
        knot(g, s, 0, 0, w * 1.1);
        fork(g, s, -1.7, H + 0.1, 150, 0.5, w * 0.35);
        fork(g, s, 1.7, -H - 0.1, -30, 0.5, w * 0.35);
        fork(g, s, -1.7, -H - 0.1, -150, 0.5, w * 0.35);
        fork(g, s, 1.7, H + 0.1, 30, 0.5, w * 0.35);
        twig(g, s, 0.75, -0.42, -95, 0.5, w * 0.35, true);
        twig(g, s, -0.8, 0.45, 95, 0.5, w * 0.35, true);
        return g;
    }

    // ramo principale: curva di Bezier (x0,y0, c1, c2, fine) con corteccia scura e un filo di luce
    private static void limb(Group g, double s, double width, double... p) {
        Path dark = curve(s, p);
        dark.setStroke(BARK);
        dark.setStrokeWidth(width * s);
        Path light = curve(s, p);
        light.setStroke(BARK_LIGHT);
        light.setStrokeWidth(width * s * 0.3);
        light.setTranslateX(-width * s * 0.18);
        light.setTranslateY(-width * s * 0.12);
        g.getChildren().addAll(dark, light);
        g.setEffect(new DropShadow(s * 0.18, s * 0.05, s * 0.08, Color.color(0.1, 0.15, 0.1, 0.45)));
    }

    // rametto da (x, y) nella direzione indicata (gradi sullo schermo), con una fogliolina in punta se richiesta
    private static void twig(Group g, double s, double x, double y, double angle, double length, double width, boolean leaf) {
        double a = Math.toRadians(angle);
        double ex = x + Math.cos(a) * length;
        double ey = y + Math.sin(a) * length;
        QuadCurve stick = new QuadCurve(x * s, y * s,
                (x + Math.cos(a + 0.35) * length * 0.55) * s, (y + Math.sin(a + 0.35) * length * 0.55) * s,
                ex * s, ey * s);
        stick.setFill(null);
        stick.setStroke(BARK);
        stick.setStrokeWidth(width * s);
        stick.setStrokeLineCap(StrokeLineCap.ROUND);
        g.getChildren().add(stick);
        if (leaf) {
            Ellipse bud = new Ellipse(ex * s, ey * s, 0.13 * s, 0.07 * s);
            bud.setRotate(angle);
            bud.setFill(FOLIAGE);
            bud.setStroke(FOLIAGE_DARK);
            g.getChildren().add(bud);
        }
    }

    // punta del ramo che si apre in due
    private static void fork(Group g, double s, double x, double y, double angle, double length, double width) {
        twig(g, s, x, y, angle - 28, length, width, false);
        twig(g, s, x, y, angle + 28, length * 0.8, width, true);
    }

    private static void knot(Group g, double s, double x, double y, double width) {
        Circle knot = new Circle(x * s, y * s, width * s * 0.75, BARK);
        Circle ring = new Circle(x * s, y * s, width * s * 0.35, null);
        ring.setStroke(BARK_LIGHT);
        ring.setStrokeWidth(Math.max(1, s * 0.03));
        g.getChildren().addAll(knot, ring);
    }

    private static Path curve(double s, double... p) {
        Path path = new Path(new MoveTo(p[0] * s, p[1] * s));
        for (int i = 2; i + 5 < p.length; i += 6) {
            path.getElements().add(new CubicCurveTo(p[i] * s, p[i + 1] * s, p[i + 2] * s, p[i + 3] * s,
                    p[i + 4] * s, p[i + 5] * s));
        }
        path.setFill(null);
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        path.setStrokeLineJoin(StrokeLineJoin.ROUND);
        return path;
    }

    // ---------------------------------------------------------------- shishi odoshi

    // canna di bambu' basculante su una vaschetta di pietra; i tre esagoni formano un triangolo
    private static Group shishiOdoshi(double s) {
        Group g = new Group();

        // vaschetta: anello di sassi attorno a un po' d'acqua
        double[][] rocks = {{-0.75, 0.25, 0.3}, {-0.4, 0.62, 0.28}, {0.1, 0.72, 0.3}, {0.6, 0.55, 0.3},
                {0.85, 0.15, 0.26}, {-0.6, -0.15, 0.24}, {0.7, -0.25, 0.22}};
        for (double[] r : rocks) {
            Ellipse rock = new Ellipse(r[0] * s, r[1] * s, r[2] * s, r[2] * s * 0.72);
            rock.setFill(KoiArt.stoneFill(r[0] * s, r[1] * s, r[2] * s));
            rock.setStroke(STONE_DARK);
            g.getChildren().add(rock);
        }
        Ellipse pool = new Ellipse(0.08 * s, 0.25 * s, 0.6 * s, 0.32 * s);
        pool.setFill(KoiArt.waterFill(0.08 * s, 0.25 * s, 0.6 * s, false));
        pool.setStroke(STONE_DARK);
        g.getChildren().add(pool);

        // sostegni
        for (double x : new double[] {-0.38, 0.3}) {
            g.getChildren().add(bamboo(s, x, -1.0, 0.1, 1.2, false));
        }

        // canna basculante: perno al centro, bocca aperta verso la vaschetta
        Group tube = new Group(bamboo(s, -0.95, -0.12, 1.9, 0.24, true));
        Ellipse mouth = new Ellipse(0.95 * s, 0, 0.06 * s, 0.12 * s);
        mouth.setFill(BAMBOO_DARK.darker());
        tube.getChildren().add(mouth);
        tube.getTransforms().addAll(new Translate(0, -0.55 * s), new Rotate(18));
        Circle pivot = new Circle(0, -0.55 * s, 0.06 * s, BARK);

        // cannello che versa acqua nella parte alta della canna
        Group spout = new Group(bamboo(s, -0.32, -0.05, 0.64, 0.1, true));
        spout.getTransforms().addAll(new Translate(-1.15 * s, -1.25 * s), new Rotate(25));
        Line stream = new Line(-0.87 * s, -1.1 * s, -0.86 * s, -0.92 * s);
        stream.setStroke(WATER);
        stream.setStrokeWidth(0.05 * s);
        stream.setStrokeLineCap(StrokeLineCap.ROUND);
        Circle drop = new Circle(0.98 * s, 0.05 * s, 0.05 * s, WATER);

        g.getChildren().addAll(tube, pivot, spout, stream, drop);
        g.setEffect(new DropShadow(s * 0.15, s * 0.04, s * 0.06, Color.color(0.1, 0.15, 0.1, 0.4)));
        return g;
    }

    // canna di bambu' dritta con i nodi; orizzontale o verticale
    private static Group bamboo(double s, double x, double y, double w, double h, boolean horizontal) {
        Rectangle cane = new Rectangle(x * s, y * s, w * s, h * s);
        cane.setArcWidth(Math.min(w, h) * s * 0.6);
        cane.setArcHeight(Math.min(w, h) * s * 0.6);
        cane.setFill(new LinearGradient(0, 0, horizontal ? 0 : 1, horizontal ? 1 : 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, BAMBOO.brighter()), new Stop(0.5, BAMBOO), new Stop(1, BAMBOO_DARK)));
        cane.setStroke(BAMBOO_DARK);
        cane.setStrokeWidth(Math.max(0.8, s * 0.02));
        Group g = new Group(cane);
        for (double t : new double[] {0.33, 0.67}) {
            Line ring = horizontal
                    ? new Line((x + w * t) * s, y * s, (x + w * t) * s, (y + h) * s)
                    : new Line(x * s, (y + h * t) * s, (x + w) * s, (y + h * t) * s);
            ring.setStroke(BAMBOO_DARK);
            ring.setStrokeWidth(Math.max(0.8, s * 0.03));
            g.getChildren().add(ring);
        }
        return g;
    }

    // ---------------------------------------------------------------- carpa centenaria

    // carpa farfalla dorata con pinne lunghe e velate: dalla coda (esagoni in basso) alla testa (esagono in alto a destra)
    private static Group oldCarp(double s) {
        double length = 3.5 * s;
        double l = length;

        Paint veil = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(1, 0.55, 0.25, 0.25)), new Stop(1, Color.color(1, 0.98, 0.92, 0.85)));

        Path tail = new Path(new MoveTo(-0.22 * l, 0),
                new CubicCurveTo(-0.4 * l, -0.05 * l, -0.55 * l, -0.32 * l, -0.7 * l, -0.26 * l),
                new CubicCurveTo(-0.6 * l, -0.12 * l, -0.58 * l, -0.02 * l, -0.5 * l, 0),
                new CubicCurveTo(-0.58 * l, 0.02 * l, -0.6 * l, 0.12 * l, -0.7 * l, 0.26 * l),
                new CubicCurveTo(-0.55 * l, 0.32 * l, -0.4 * l, 0.05 * l, -0.22 * l, 0),
                new ClosePath());
        tail.setFill(veil);
        tail.setStroke(Color.color(0.85, 0.4, 0.15, 0.5));

        Group fins = new Group();
        for (int side : new int[] {-1, 1}) {
            Path fin = new Path(new MoveTo(0.14 * l, side * 0.08 * l),
                    new CubicCurveTo(0.08 * l, side * 0.2 * l, -0.05 * l, side * 0.3 * l, -0.16 * l, side * 0.32 * l),
                    new CubicCurveTo(-0.06 * l, side * 0.22 * l, 0.02 * l, side * 0.14 * l, 0.06 * l, side * 0.08 * l),
                    new ClosePath());
            fin.setFill(veil);
            fin.setStroke(Color.color(0.85, 0.4, 0.15, 0.45));
            fins.getChildren().add(fin);
        }

        Group body = KoiArt.koi(l * 0.82, 2);   // dorata: si distingue dalle carpe normali

        Group barbels = new Group();
        for (int side : new int[] {-1, 1}) {
            QuadCurve whisker = new QuadCurve(0.34 * l, side * 0.03 * l, 0.4 * l, side * 0.04 * l, 0.44 * l, side * 0.1 * l);
            whisker.setFill(null);
            whisker.setStroke(Color.web("#8a5a2b"));
            whisker.setStrokeWidth(Math.max(1, s * 0.03));
            barbels.getChildren().add(whisker);
        }

        Group sparkle = new Group();
        double[][] scales = {{0.1, -0.06}, {-0.02, -0.07}, {0.04, 0.02}, {-0.12, 0.0}};
        for (double[] p : scales) {
            sparkle.getChildren().add(new Circle(p[0] * l, p[1] * l, s * 0.05, Color.color(1, 0.86, 0.4, 0.85)));
        }

        Group fish = new Group(tail, fins, body, barbels, sparkle);
        fish.setEffect(new DropShadow(s * 0.35, Color.color(1, 0.8, 0.3, 0.75)));
        // asse dalla coda (tra SW e S dell'ancoraggio) alla testa (NE), coordinate dal baricentro
        fish.getTransforms().addAll(new Translate(0.3 * s, -0.05 * s), new Rotate(-45));
        return new Group(fish);
    }

    // ---------------------------------------------------------------- tartaruga

    private static Group turtle(double s) {
        Ellipse rock = new Ellipse(0, 0.12 * s, 0.8 * s, 0.52 * s);
        rock.setFill(KoiArt.stoneFill(0, 0.12 * s, 0.8 * s));
        rock.setStroke(STONE_DARK);

        Color skin = Color.web("#86ad55");
        Group animal = new Group();
        for (double[] p : new double[][] {{0.3, -0.3}, {0.3, 0.3}, {-0.28, -0.3}, {-0.28, 0.3}}) {
            Ellipse leg = new Ellipse(p[0] * s, p[1] * s, 0.13 * s, 0.09 * s);
            leg.setFill(skin);
            leg.setStroke(FOLIAGE_DARK);
            animal.getChildren().add(leg);
        }
        Polygon tail = new Polygon(-0.42 * s, -0.05 * s, -0.6 * s, 0, -0.42 * s, 0.05 * s);
        tail.setFill(skin);
        Ellipse head = new Ellipse(0.53 * s, 0, 0.17 * s, 0.13 * s);
        head.setFill(skin);
        head.setStroke(FOLIAGE_DARK);
        Circle eye = new Circle(0.6 * s, -0.05 * s, Math.max(1, 0.025 * s), Theme.INK);

        Ellipse shell = new Ellipse(0, 0, 0.45 * s, 0.35 * s);
        shell.setFill(new RadialGradient(0, 0, -0.12 * s, -0.1 * s, 0.55 * s, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#7fae45")), new Stop(1, Color.web("#2f5a1c"))));
        shell.setStroke(Color.web("#24451a"));
        shell.setStrokeWidth(Math.max(1, s * 0.03));

        // scudi del carapace: un esagono al centro e le linee verso il bordo
        Group scutes = new Group();
        Polygon middle = KoiArt.hexagon(0, 0, 0.15 * s);
        middle.setFill(null);
        middle.setStroke(Color.web("#d6e4a0"));
        scutes.getChildren().add(middle);
        for (int corner = 0; corner < 6; corner++) {
            double a = Math.toRadians(60 * corner);
            Line seam = new Line(Math.cos(a) * 0.15 * s, Math.sin(a) * 0.15 * s, Math.cos(a) * 0.45 * s, Math.sin(a) * 0.35 * s);
            seam.setStroke(Color.web("#d6e4a0", 0.8));
            scutes.getChildren().add(seam);
        }
        scutes.setClip(new Ellipse(0, 0, 0.45 * s, 0.35 * s));

        animal.getChildren().addAll(tail, head, eye, shell, scutes);
        animal.setRotate(-25);
        animal.setEffect(new DropShadow(s * 0.15, s * 0.04, s * 0.06, Color.color(0.1, 0.15, 0.1, 0.45)));
        return new Group(rock, animal);
    }

    // ---------------------------------------------------------------- salice piangente

    private static Group willow(double s) {
        Ellipse ground = new Ellipse(0, 0.58 * s, 0.75 * s, 0.2 * s);
        ground.setFill(Color.web("#8fb55a"));
        ground.setStroke(FOLIAGE_DARK);

        Path trunk = curve(s, 0.05, 0.6, -0.15, 0.3, 0.2, -0.05, -0.05, -0.45);
        trunk.setStroke(BARK);
        trunk.setStrokeWidth(0.17 * s);

        Ellipse crown = new Ellipse(0, -0.5 * s, 0.62 * s, 0.24 * s);
        crown.setFill(FOLIAGE_DARK);

        // rami che ricadono, alternando due verdi
        Group fronds = new Group();
        for (int i = -6; i <= 6; i++) {
            double x0 = i * 0.1;
            double top = -0.62 + Math.abs(i) * 0.03;
            double bottom = 0.32 - Math.abs(i) * 0.035;
            QuadCurve frond = new QuadCurve(x0 * s, top * s, x0 * 1.5 * s, (top + 0.2) * s, x0 * 1.2 * s, bottom * s);
            frond.setFill(null);
            frond.setStroke(i % 2 == 0 ? FOLIAGE : Color.web("#a9cf62"));
            frond.setStrokeWidth(0.075 * s);
            frond.setStrokeLineCap(StrokeLineCap.ROUND);
            fronds.getChildren().add(frond);
        }

        Group g = new Group(ground, trunk, crown, fronds);
        g.setEffect(new DropShadow(s * 0.15, s * 0.04, s * 0.06, Color.color(0.1, 0.15, 0.1, 0.4)));
        return g;
    }

    // ---------------------------------------------------------------- cascata

    private static Group waterfall(double s) {
        Polygon cliff = new Polygon(
                -0.82 * s, 0.32 * s, -0.78 * s, -0.5 * s, -0.42 * s, -0.8 * s, 0.02 * s, -0.68 * s,
                0.4 * s, -0.86 * s, 0.8 * s, -0.5 * s, 0.84 * s, 0.32 * s);
        cliff.setFill(KoiArt.stoneFill(0, -0.3 * s, 0.9 * s));
        cliff.setStroke(STONE_DARK);

        Group moss = new Group();
        for (double[] p : new double[][] {{-0.55, -0.62}, {-0.2, -0.72}, {0.45, -0.72}, {0.65, -0.45}}) {
            Ellipse tuft = new Ellipse(p[0] * s, p[1] * s, 0.16 * s, 0.07 * s);
            tuft.setFill(FOLIAGE);
            moss.getChildren().add(tuft);
        }

        Rectangle fall = new Rectangle(-0.24 * s, -0.66 * s, 0.48 * s, 0.92 * s);
        fall.setArcWidth(0.12 * s);
        fall.setArcHeight(0.12 * s);
        fall.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, WATER_LIGHT), new Stop(1, WATER)));
        Group streaks = new Group();
        for (double x : new double[] {-0.14, -0.03, 0.08, 0.17}) {
            Line streak = new Line(x * s, -0.6 * s, x * s, 0.2 * s);
            streak.setStroke(Color.color(1, 1, 1, 0.7));
            streak.setStrokeWidth(Math.max(1, s * 0.03));
            streaks.getChildren().add(streak);
        }

        Ellipse pool = new Ellipse(0, 0.42 * s, 0.72 * s, 0.22 * s);
        pool.setFill(KoiArt.waterFill(0, 0.42 * s, 0.72 * s, false));
        pool.setStroke(STONE_DARK);
        Group foam = new Group();
        for (double[] p : new double[][] {{-0.28, 0.3}, {-0.1, 0.36}, {0.1, 0.33}, {0.26, 0.3}, {0, 0.26}}) {
            foam.getChildren().add(new Circle(p[0] * s, p[1] * s, 0.08 * s, Color.color(1, 1, 1, 0.85)));
        }
        Ellipse leftRock = new Ellipse(-0.62 * s, 0.45 * s, 0.22 * s, 0.15 * s);
        Ellipse rightRock = new Ellipse(0.6 * s, 0.48 * s, 0.2 * s, 0.13 * s);
        for (Ellipse rock : new Ellipse[] {leftRock, rightRock}) {
            rock.setFill(STONE);
            rock.setStroke(STONE_DARK);
        }

        Group g = new Group(cliff, moss, fall, streaks, pool, foam, leftRock, rightRock);
        g.setEffect(new DropShadow(s * 0.15, s * 0.04, s * 0.06, Color.color(0.1, 0.15, 0.1, 0.4)));
        return g;
    }

    // ---------------------------------------------------------------- ponticello

    // ponte ad arco laccato di rosso, con ringhiera e pomelli dorati
    private static Group bridge(double s) {
        double ax = -0.95, bx = 0.95, cy = -0.6;   // arco: estremi a y = 0.2, controllo in alto
        Path deck = new Path(new MoveTo(ax * s, 0.2 * s),
                new QuadCurveTo(0, cy * s, bx * s, 0.2 * s),
                new LineTo(bx * s, 0.34 * s),
                new QuadCurveTo(0, (cy + 0.16) * s, ax * s, 0.34 * s),
                new ClosePath());
        deck.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, LACQUER.brighter()), new Stop(1, LACQUER_DARK)));
        deck.setStroke(LACQUER_DARK);

        QuadCurve rail = new QuadCurve(-0.88 * s, -0.08 * s, 0, -0.98 * s, 0.88 * s, -0.08 * s);
        rail.setFill(null);
        rail.setStroke(LACQUER);
        rail.setStrokeWidth(0.08 * s);
        rail.setStrokeLineCap(StrokeLineCap.ROUND);

        Group posts = new Group();
        for (int i = 0; i <= 4; i++) {
            double u = 0.06 + i * 0.22;
            double deckY = quad(0.2, cy, 0.2, u);
            double railY = quad(-0.08, -0.98, -0.08, u);
            double x = quad(-0.9, 0, 0.9, u);
            Line post = new Line(x * s, deckY * s, x * s, railY * s);
            post.setStroke(LACQUER_DARK);
            post.setStrokeWidth(0.06 * s);
            posts.getChildren().add(post);
            if (i == 0 || i == 4) {
                posts.getChildren().add(new Circle(x * s, (railY - 0.06) * s, 0.065 * s, Theme.GOLD));
            }
        }

        Rectangle leftFoot = new Rectangle(-1.05 * s, 0.2 * s, 0.22 * s, 0.2 * s);
        Rectangle rightFoot = new Rectangle(0.83 * s, 0.2 * s, 0.22 * s, 0.2 * s);
        for (Rectangle foot : new Rectangle[] {leftFoot, rightFoot}) {
            foot.setFill(STONE);
            foot.setStroke(STONE_DARK);
        }
        Ellipse reflection = new Ellipse(0, 0.48 * s, 0.6 * s, 0.07 * s);
        reflection.setFill(Color.color(0.78, 0.2, 0.12, 0.25));

        Group g = new Group(reflection, leftFoot, rightFoot, deck, posts, rail);
        g.setEffect(new DropShadow(s * 0.15, s * 0.04, s * 0.06, Color.color(0.1, 0.15, 0.1, 0.45)));
        return g;
    }

    // punto della curva quadratica a, controllo c, b al parametro u
    private static double quad(double a, double c, double b, double u) {
        return (1 - u) * (1 - u) * a + 2 * u * (1 - u) * c + u * u * b;
    }

    // ---------------------------------------------------------------- statua

    // statua di pietra seduta su un fiore di loto, con un'aureola appena dorata
    private static Group statue(double s) {
        Color stone = Color.web("#a2a89e");
        Color stoneDark = Color.web("#666c63");

        Circle halo = new Circle(0, -0.36 * s, 0.36 * s);
        halo.setFill(new RadialGradient(0, 0, 0, -0.36 * s, 0.36 * s, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(1, 0.9, 0.55, 0.55)), new Stop(1, Color.color(1, 0.9, 0.55, 0))));

        Group lotus = new Group();
        for (int i = -3; i <= 3; i++) {
            Ellipse petal = new Ellipse(i * 0.16 * s, 0.52 * s, 0.12 * s, 0.1 * s);
            petal.setFill(Color.web("#c9cfc4"));
            petal.setStroke(stoneDark);
            lotus.getChildren().add(petal);
        }
        Ellipse plinth = new Ellipse(0, 0.6 * s, 0.62 * s, 0.11 * s);
        plinth.setFill(stone.darker());
        plinth.setStroke(stoneDark);

        Path robe = new Path(new MoveTo(-0.46 * s, 0.42 * s),
                new CubicCurveTo(-0.48 * s, 0.05 * s, -0.3 * s, -0.12 * s, -0.16 * s, -0.17 * s),
                new LineTo(0.16 * s, -0.17 * s),
                new CubicCurveTo(0.3 * s, -0.12 * s, 0.48 * s, 0.05 * s, 0.46 * s, 0.42 * s),
                new ClosePath());
        robe.setFill(KoiArt.stoneFill(0, 0.1 * s, 0.6 * s));
        robe.setStroke(stoneDark);
        Ellipse lap = new Ellipse(0, 0.38 * s, 0.5 * s, 0.12 * s);
        lap.setFill(stone);
        lap.setStroke(stoneDark);
        Ellipse hands = new Ellipse(0, 0.27 * s, 0.13 * s, 0.065 * s);
        hands.setFill(stone.brighter());
        hands.setStroke(stoneDark);

        Circle head = new Circle(0, -0.36 * s, 0.17 * s);
        head.setFill(KoiArt.stoneFill(0, -0.36 * s, 0.17 * s));
        head.setStroke(stoneDark);
        Circle topknot = new Circle(0, -0.55 * s, 0.07 * s, stone);
        topknot.setStroke(stoneDark);
        Group eyes = new Group();
        for (int side : new int[] {-1, 1}) {
            QuadCurve eye = new QuadCurve(side * 0.1 * s, -0.37 * s, side * 0.06 * s, -0.33 * s, side * 0.02 * s, -0.37 * s);
            eye.setFill(null);
            eye.setStroke(stoneDark);
            eye.setStrokeWidth(Math.max(0.8, s * 0.02));
            eyes.getChildren().add(eye);
        }

        Group g = new Group(halo, plinth, lotus, robe, lap, hands, head, topknot, eyes);
        g.setEffect(new DropShadow(s * 0.15, s * 0.04, s * 0.06, Color.color(0.1, 0.15, 0.1, 0.45)));
        return g;
    }
}
