package client;

import java.util.List;
import java.util.Scanner;

import network.GameView;

public class TextView implements View {

    private final Scanner input = new Scanner(System.in);

    public void run(ClientController controller) {
        if (!controller.connect("localhost", 1099)) {
            return;
        }

        System.out.println("Enter username");
        String nickname = input.nextLine().trim();
        printHelp();
        controller.refreshRooms();

        while (true) {
            String[] parts = input.nextLine().trim().split("\\s+", 2);
            String command = parts[0].toLowerCase();
            String argument = parts.length > 1 ? parts[1] : "";

            switch (command) {
                case "rooms":
                    controller.refreshRooms();
                    break;
                case "create":
                    if (argument.isEmpty()) {
                        showError("Usage: create <room name>");
                    } else {
                        controller.createRoom(argument);
                    }
                    break;
                case "join":
                    if (argument.isEmpty()) {
                        showError("Usage: join <room id>");
                    } else {
                        controller.joinRoom(argument, nickname);
                    }
                    break;
                case "start":
                    controller.startGame();
                    break;
                case "draw":
                    try {
                        controller.drawCard(Integer.parseInt(argument));
                    } catch (NumberFormatException e) {
                        showError("Usage: draw <position>, for example: draw 2");
                    }
                    break;
                case "choose":
                    try {
                        controller.choose(Integer.parseInt(argument));
                    } catch (NumberFormatException e) {
                        showError("Usage: choose <number>, for example: choose 0");
                    }
                    break;
                case "water":
                    int[] water = parseNumbers(argument, "water <q> <r>", 2);
                    if (water != null) {
                        controller.placeWater(water[0], water[1]);
                    }
                    break;
                case "leaf":
                    int[] leaf = parseNumbers(argument, "leaf <q> <r>", 2);
                    if (leaf != null) {
                        controller.placeLeaf(leaf[0], leaf[1]);
                    }
                    break;
                case "lily":
                    int[] lily = parseNumbers(argument, "lily <q> <r>", 2);
                    if (lily != null) {
                        controller.placeLily(lily[0], lily[1]);
                    }
                    break;
                case "border":
                    int[] border = parseNumbers(argument, "border <q> <r> <angle>", 3);
                    if (border != null) {
                        controller.placeBorder(border[0], border[1], border[2]);
                    }
                    break;
                case "lantern":
                    int[] lantern = parseNumbers(argument, "lantern <q> <r>", 2);
                    if (lantern != null) {
                        controller.placeLantern(lantern[0], lantern[1]);
                    }
                    break;
                case "koi":
                    int[] koi = parseNumbers(argument, "koi <type> <q> <r> <angle>", 4);
                    if (koi != null) {
                        controller.placeKoi(koi[0], koi[1], koi[2], koi[3]);
                    }
                    break;                 
                case "end":
                    controller.endTurn();
                    break;
                case "quit":
                    controller.quit();
                    break;
                case "help":
                    printHelp();
                    break;
                case "":
                    break;
                default:
                    showError("Unknown command '" + command + "', type 'help'");
            }
        }
    }

    private int[] parseNumbers(String argument, String usage, int count) {
        String[] parts = argument.split("\\s+");
        try {
            int[] numbers = new int[count];
            for (int i = 0; i < count; i++) {
                numbers[i] = Integer.parseInt(parts[i]);
            }
            return numbers;
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            showError("Usage: " + usage);
            return null;
        }
    }

    private void printHelp() {
        System.out.println("Commands: rooms | create <name> | join <room id> | start | draw <position> | choose <number> | water <q> <r> | leaf  <q> <r> | lily  <q> <r> | border <q> <r> <rotation> | lantern <q> <r> | koi <type> <q> <r> <rotation> | end | quit | help");
    }

    @Override
    public void showLobby(List<String> rooms) {
        if (rooms.isEmpty()) {
            System.out.println("No rooms yet, create one with: create <name>");
        } else {
            System.out.println("Rooms: " + rooms);
        }
    }

    @Override
    public void showRoom(String roomId) {
        System.out.println("Joined room " + roomId + ". Type 'start' when everyone is in.");
    }

    @Override
    public void showMessage(String text) {
        System.out.println("[server] " + text);
    }

    @Override
    public void showError(String text) {
        System.out.println("[error] " + text);
    }

    @Override
    public void showGame(GameView view) {
        System.out.println(GameViewPrinter.render(view));
    }

}
