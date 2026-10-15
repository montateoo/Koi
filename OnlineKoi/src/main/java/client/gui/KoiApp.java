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
        stage.setOnCloseRequest(e -> view.inBackground(controller::quit));
        stage.show();

        view.inBackground(() -> {
            if (controller.connect("localhost", 1099)) {
                controller.refreshRooms();
            }
        });
    }
}
