package client.gui;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.image.Image;

// icona dell'applicazione (barra del titolo e barra delle applicazioni): una carpa kohaku in un quadrato d'acqua.
// Piu' misure in assets/icons, cosi' il sistema sceglie la piu' nitida per ogni posto
final class AppIcon {

    private static final int[] SIZES = {16, 24, 32, 48, 64, 128, 256};

    private AppIcon() {
    }

    // le misure che mancano si saltano: senza icone la finestra usa quella di sistema, ma il gioco parte lo stesso
    static List<Image> images() {
        List<Image> images = new ArrayList<>();
        for (int px : SIZES) {
            InputStream file = AppIcon.class.getResourceAsStream("/assets/icons/koi-" + px + ".png");
            if (file != null) {
                images.add(new Image(file));
            }
        }
        return images;
    }
}
