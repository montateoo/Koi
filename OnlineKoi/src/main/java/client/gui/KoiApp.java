package client.gui;

import client.ClientController;
import javafx.application.Application;
import javafx.stage.Stage;

public class KoiApp extends Application {

    @Override
    public void start(Stage stage) {
        GuiView view = new GuiView(stage);
        ClientController controller = new ClientController(view);
        view.setController(controller);

        stage.setTitle("Koi");
        stage.getIcons().addAll(AppIcon.images());
        stage.setOnCloseRequest(e -> view.inBackground(controller::quit));
        stage.setMaximized(true);   // lo sfondo riempie tutto lo schermo, qualunque sia la sua misura
        stage.show();
        view.startIntro();

        view.inBackground(() -> controller.connect(Settings.host(), Settings.port()));
    }
}
