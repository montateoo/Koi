package client.gui;

import java.util.List;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

// palette e font della GUI, ispirati ai colori del regolamento: carta, acqua turchese, legno, pietra, rosso vermiglio
final class Theme {

    static final Color PAPER = Color.web("#f6efdc");
    static final Color PAPER_LIGHT = Color.web("#fbf7ec");
    static final Color PAPER_DARK = Color.web("#e4d6b4");
    static final Color INK = Color.web("#2b2622");
    static final Color INK_SOFT = Color.web("#7a6c5a");
    static final Color VERMILION = Color.web("#c8341f");
    static final Color GOLD = Color.web("#d4a13a");
    static final Color TEAL_LIGHT = Color.web("#c3ecf1");
    static final Color TEAL = Color.web("#6cc3cf");
    static final Color TEAL_DEEP = Color.web("#2e7f8f");
    static final Color WOOD = Color.web("#9a7048");
    static final Color WOOD_DARK = Color.web("#6b4a2e");
    static final Color LEAF = Color.web("#7cb35a");
    static final Color STONE = Color.web("#cbbfa6");
    static final Color STONE_DARK = Color.web("#8f8574");
    static final Color SAKURA = Color.web("#f7b6c8");

    // il primo font disponibile sul sistema; su Windows i font giapponesi di solito ci sono
    private static final String JAPANESE = pick(List.of("Yu Mincho", "MS Mincho", "Yu Gothic", "Meiryo", "MS Gothic"));
    private static final String SERIF = pick(List.of("Yu Mincho", "Georgia", "Times New Roman"));

    private Theme() {
    }

    private static String pick(List<String> candidates) {
        List<String> installed = Font.getFamilies();
        for (String family : candidates) {
            if (installed.contains(family)) {
                return family;
            }
        }
        return null;
    }

    static boolean hasJapanese() {
        return JAPANESE != null;
    }

    static Font kanji(double size) {
        return JAPANESE != null ? Font.font(JAPANESE, FontWeight.BOLD, size) : Font.font("Serif", FontWeight.BOLD, size);
    }

    static Font title(double size) {
        return SERIF != null ? Font.font(SERIF, FontWeight.BOLD, size) : Font.font("Serif", FontWeight.BOLD, size);
    }

    static Font serif(double size) {
        return SERIF != null ? Font.font(SERIF, size) : Font.font("Serif", size);
    }
}
