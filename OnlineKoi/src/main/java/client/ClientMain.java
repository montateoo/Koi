package client;

public class ClientMain {
    public static void main(String[] args) {

        TextView view = new TextView();
        ClientController controller = new ClientController(view);
        view.run(controller);

    }
}
