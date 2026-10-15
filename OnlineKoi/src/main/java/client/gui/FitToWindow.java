package client.gui;

import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.transform.Scale;

// impagina il contenuto come se la finestra fosse grande almeno designWidth x designHeight:
// sugli schermi piccoli (o con il ridimensionamento di Windows) rimpicciolisce tutto in modo uniforme
// invece di tagliare i bordi; sugli schermi grandi il contenuto riempie semplicemente la finestra
public class FitToWindow extends Pane {

    private final Region content;
    private final double designWidth;
    private final double designHeight;
    private final Scale scale = new Scale(1, 1, 0, 0);

    public FitToWindow(Region content, double designWidth, double designHeight) {
        this.content = content;
        this.designWidth = designWidth;
        this.designHeight = designHeight;
        content.getTransforms().add(scale);
        getChildren().add(content);
    }

    @Override
    protected void layoutChildren() {
        double width = getWidth();
        double height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        double factor = Math.min(1, Math.min(width / designWidth, height / designHeight));
        scale.setX(factor);
        scale.setY(factor);
        // il contenuto viene impaginato piu' grande e poi rimpicciolito: cosi' riempie sempre tutta la finestra
        content.resizeRelocate(0, 0, width / factor, height / factor);
    }
}
