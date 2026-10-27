package client;

import java.util.List;
import java.util.Scanner;

import game.scoring.ScoreSheet;
import network.GameView;
import network.MatchRecord;

public class TextView implements View {

    private final Scanner input = new Scanner(System.in);

    public void run(ClientController controller) {
        if (!controller.connect("localhost", 1099)) {
            return;
        }

        System.out.println("Scrivi il tuo nome");
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
                        showError("Uso: create <nome stanza>");
                    } else {
                        controller.createRoom(argument);
                    }
                    break;
                case "join":
                    if (argument.isEmpty()) {
                        showError("Uso: join <id stanza>");
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
                        showError("Uso: draw <posizione>, per esempio: draw 2");
                    }
                    break;
                case "choose":
                    try {
                        controller.choose(Integer.parseInt(argument));
                    } catch (NumberFormatException e) {
                        showError("Uso: choose <numero>, per esempio: choose 0");
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
                    int[] border = parseNumbers(argument, "border <q> <r> <rotazione>", 3);
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
                    int[] koi = parseNumbers(argument, "koi <tipo> <q> <r> <rotazione>", 4);
                    if (koi != null) {
                        controller.placeKoi(koi[0], koi[1], koi[2], koi[3]);
                    }
                    break;
                case "move":
                    int[] moveKoi = parseNumbers(argument, "move <id carpa> <q> <r> <rotazione>", 4);
                    if (moveKoi != null) {
                        controller.moveKoi(moveKoi[0], moveKoi[1], moveKoi[2], moveKoi[3]);
                    }
                    break;                 
                case "end":
                    controller.endTurn();
                    break;
                case "lucky":
                    controller.spendLuckyCard();
                    break;
                case "leave":
                    controller.leaveRoom();
                    System.out.println("Sei uscito dalla stanza. Scrivi 'rooms' per vedere le stanze aperte.");
                    break;
                case "history":
                    controller.requestHistory();
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
                    showError("Comando sconosciuto '" + command + "', scrivi 'help'");
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
            showError("Uso: " + usage);
            return null;
        }
    }

    private void printHelp() {
        System.out.println("Comandi: rooms (stanze) | create <nome> | join <id stanza> | start | draw <posizione> (prendi una carta) | choose <numero> (rispondi) | water/leaf/lily/lantern <q> <r> (acqua, fogliame, ninfea, lanterna) | border <q> <r> <rotazione> (sponda) | koi <tipo 0-2> <q> <r> <rotazione> | move <id carpa> <q> <r> <rotazione> | lucky (segnalino Fortuna) | end (passa il turno) | leave (esci dalla stanza) | history (storico) | quit | help");
    }

    @Override
    public void showLobby(List<String> rooms) {
        if (rooms.isEmpty()) {
            System.out.println("Nessuna stanza: creane una con create <nome>");
        } else {
            System.out.println("Stanze: " + rooms);
        }
    }

    @Override
    public void showRoom(String roomId) {
        System.out.println("Sei nella stanza " + roomId + ". Scrivi 'start' quando ci sono tutti.");
    }

    @Override
    public void showMessage(String text) {
        System.out.println("[server] " + text);
    }

    @Override
    public void showError(String text) {
        System.out.println("[errore] " + text);
    }

    @Override
    public void showGame(GameView view) {
        System.out.println(GameViewPrinter.render(view));
    }

    @Override
    public void showGameOver(List<ScoreSheet> ranking) {
        System.out.println(GameViewPrinter.renderScores(ranking));
    }

    @Override
    public void showHistory(List<MatchRecord> matches) {
        System.out.println(GameViewPrinter.renderHistory(matches));
    }

}
