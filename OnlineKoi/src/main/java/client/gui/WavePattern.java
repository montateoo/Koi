package client.gui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;

// sfondo seigaiha (onde a scaglie del mare), il motivo tradizionale giapponese; si ridisegna quando cambia dimensione
public class WavePattern extends Region {

    private static final double RADIUS = 30;

    private final Canvas canvas = new Canvas();
    private final Color background;
    private final Color line;

    public WavePattern(Color background, Color line) {
        this.background = background;
        this.line = line;
        getChildren().add(canvas);
        setMouseTransparent(true);
        widthProperty().addListener((obs, old, value) -> redraw());
        heightProperty().addListener((obs, old, value) -> redraw());
    }

    private void redraw() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        canvas.setWidth(w);
        canvas.setHeight(h);
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(background);
        g.fillRect(0, 0, w, h);
        g.setStroke(line);
        g.setLineWidth(1.2);

        // righe dall'alto in basso: ogni scaglia copre in parte quelle della riga sopra
        int row = 0;
        for (double y = -RADIUS; y < h + RADIUS; y += RADIUS / 2, row++) {
            double offset = (row % 2 == 0) ? 0 : RADIUS;
            for (double x = -RADIUS + offset; x < w + RADIUS; x += RADIUS * 2) {
                g.setFill(background);
                g.fillArc(x - RADIUS, y - RADIUS, RADIUS * 2, RADIUS * 2, 0, 180, ArcType.ROUND);
                for (double r = RADIUS; r > 4; r -= RADIUS / 4) {
                    g.strokeArc(x - r, y - r, r * 2, r * 2, 0, 180, ArcType.OPEN);
                }
            }
        }
    }
}
