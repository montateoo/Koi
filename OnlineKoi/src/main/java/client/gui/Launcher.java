package client.gui;

import javafx.application.Application;

// avviare sempre questa classe: se il main fosse dentro KoiApp, JavaFX non partirebbe in un progetto non modulare
public class Launcher {
    public static void main(String[] args) {
        Application.launch(KoiApp.class, args);
    }
}
