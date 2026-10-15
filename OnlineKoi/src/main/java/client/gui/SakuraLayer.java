package client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javafx.animation.AnimationTimer;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.SVGPath;

// petali di ciliegio che cadono ondeggiando sopra tutta la finestra; non intercettano i clic
public class SakuraLayer extends Pane {

    private final List<Petal> petals = new ArrayList<>();
    private final Random random = new Random();
    private final AnimationTimer timer;

    private static final class Petal {
        final SVGPath shape;
        double x, y, speed, sway, phase, spin;
        boolean placed;

        Petal(SVGPath shape) {
            this.shape = shape;
        }
    }

    public SakuraLayer(int count) {
        setMouseTransparent(true);
        setPickOnBounds(false);
        for (int i = 0; i < count; i++) {
            SVGPath shape = new SVGPath();
            // petalo con la tipica tacca in punta
            shape.setContent("M0 0 C3 -6 11 -7 15 -2 L12 0 L15 2 C11 7 3 6 0 0 Z");
            shape.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#ffd6e2")), new Stop(1, Color.web("#ec7fa3"))));
            shape.setStroke(Color.web("#d1668b", 0.6));
            shape.setOpacity(0.7 + random.nextDouble() * 0.3);
            double scale = 1.0 + random.nextDouble() * 0.9;
            shape.setScaleX(scale);
            shape.setScaleY(scale);
            Petal petal = new Petal(shape);
            petals.add(petal);
            getChildren().add(shape);
        }

        timer = new AnimationTimer() {
            private long last = 0;

            @Override
            public void handle(long now) {
                double dt = last == 0 ? 0 : (now - last) / 1e9;
                last = now;
                step(Math.min(dt, 0.05), now / 1e9);
            }
        };
        timer.start();
    }

    private void reset(Petal p, boolean anywhere) {
        p.x = random.nextDouble() * Math.max(1, getWidth());
        p.y = anywhere ? random.nextDouble() * Math.max(1, getHeight()) : -20 - random.nextDouble() * 60;
        p.speed = 18 + random.nextDouble() * 26;
        p.sway = 12 + random.nextDouble() * 22;
        p.phase = random.nextDouble() * Math.PI * 2;
        p.spin = (random.nextBoolean() ? 1 : -1) * (20 + random.nextDouble() * 60);
        p.placed = true;
    }

    private void step(double dt, double time) {
        if (getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        for (Petal p : petals) {
            if (!p.placed) {
                reset(p, true);
            }
            p.y += p.speed * dt;
            p.x += Math.sin(time * 0.9 + p.phase) * p.sway * dt + 6 * dt;   // un filo di vento verso destra
            p.shape.setRotate(p.shape.getRotate() + p.spin * dt);
            if (p.y > getHeight() + 20 || p.x > getWidth() + 30) {
                reset(p, false);
            }
            p.shape.setLayoutX(p.x);
            p.shape.setLayoutY(p.y);
        }
    }

    public void stop() {
        timer.stop();
    }
}
