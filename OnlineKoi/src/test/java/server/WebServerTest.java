package server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;

import io.javalin.Javalin;
import messages.Json;

// il server vero su una porta libera, con client WebSocket veri: lo stesso percorso che fa il browser
class WebServerTest {

    @TempDir
    Path dataDir;

    private Javalin app;
    private int port;

    @BeforeEach
    void start() {
        app = new WebServer(new Lobby(new HistoryStore(dataDir.resolve("history.json")))).start("127.0.0.1", 0);
        port = app.port();
    }

    @AfterEach
    void stop() {
        app.stop();
    }

    @Test
    void servesThePage() throws Exception {
        HttpResponse<String> page = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, page.statusCode());
        assertTrue(page.body().contains("<title>"), "index.html");
        HttpResponse<String> data = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/assets/cards.json")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(404, data.statusCode(), "i dati del gioco non sono pubblici");
    }

    @Test
    void twoPlayersMeetInARoom() throws Exception {
        Client alice = new Client(port);
        Client bob = new Client(port);
        assertEquals("Welcome", alice.next().get("type").asText());
        assertEquals("carpa media", bob.next().at("/tileNames/MEDIUMKOI").asText());

        alice.send("{\"type\":\"CreateAndJoinRoom\",\"name\":\"Stagno\",\"nickname\":\"Alice\"}");
        JsonNode joined = alice.until("Joined");
        assertEquals("Stagno", joined.get("roomName").asText());

        bob.send("{\"type\":\"ListRooms\"}");
        JsonNode rooms = bob.until("RoomList");
        assertEquals("Alice", rooms.at("/rooms/0/players/0").asText());

        bob.send("{\"type\":\"JoinRoom\",\"roomId\":\"" + joined.get("roomId").asText() + "\",\"nickname\":\"alice\"}");
        assertTrue(bob.until("ErrorMessage").get("text").asText().contains("già un giocatore"));
        bob.send("{\"type\":\"JoinRoom\",\"roomId\":\"" + joined.get("roomId").asText() + "\",\"nickname\":\"Bob\"}");
        bob.until("Joined");

        alice.send("{\"type\":\"StartGame\"}");
        bob.send("{\"type\":\"StartGame\"}");
        JsonNode update = alice.until("GameUpdate");
        assertEquals(2, update.at("/view/players").size());
        assertEquals("Alice", update.at("/view/viewer").asText());

        bob.send("{\"type\":\"Nonsense\"}");
        assertEquals("Messaggio non valido", bob.until("ErrorMessage").get("text").asText());

        // chi chiude la pagina durante la partita resta in gioco: la stanza resta finche' c'e' qualcuno collegato
        bob.socket.sendClose(WebSocket.NORMAL_CLOSURE, "").join();
        assertTrue(alice.until("Info").get("text").asText().contains("Bob si è scollegato"));
        alice.send("{\"type\":\"ListRooms\"}");
        assertEquals(1, alice.until("RoomList").get("rooms").size());

        // se si scollegano tutti, la stanza sparisce dalla lista
        alice.socket.sendClose(WebSocket.NORMAL_CLOSURE, "").join();
        Client carol = new Client(port);
        for (int attempt = 0; attempt < 50; attempt++) {
            carol.send("{\"type\":\"ListRooms\"}");
            if (carol.until("RoomList").get("rooms").isEmpty()) {
                break;
            }
            Thread.sleep(20);   // la chiusura di Alice arriva al server in un altro thread
        }
        carol.send("{\"type\":\"ListRooms\"}");
        assertEquals(0, carol.until("RoomList").get("rooms").size());
        assertTrue(Files.notExists(dataDir.resolve("history.json")), "nessuna partita finita, nessuno storico");
    }

    @Test
    void anEmptyWaitingRoomDisappears() throws Exception {
        Client alice = new Client(port);
        alice.send("{\"type\":\"CreateAndJoinRoom\",\"name\":\"Stagno\",\"nickname\":\"Alice\"}");
        alice.until("Joined");
        alice.send("{\"type\":\"Leave\"}");
        alice.send("{\"type\":\"ListRooms\"}");
        assertEquals(0, alice.until("RoomList").get("rooms").size());
    }

    // client di prova: mette in coda ogni messaggio ricevuto
    private static final class Client implements WebSocket.Listener {

        private final BlockingQueue<JsonNode> received = new LinkedBlockingQueue<>();
        private final StringBuilder partial = new StringBuilder();
        final WebSocket socket;

        Client(int port) {
            socket = HttpClient.newHttpClient().newWebSocketBuilder()
                    .buildAsync(URI.create("ws://127.0.0.1:" + port + "/ws"), this).join();
        }

        void send(String json) {
            socket.sendText(json, true).join();
        }

        JsonNode next() throws InterruptedException {
            JsonNode message = received.poll(5, TimeUnit.SECONDS);
            if (message == null) {
                throw new AssertionError("nessun messaggio dal server");
            }
            return message;
        }

        // salta i messaggi informativi fino a quello del tipo cercato
        JsonNode until(String type) throws InterruptedException {
            while (true) {
                JsonNode message = next();
                if (message.get("type").asText().equals(type)) {
                    return message;
                }
            }
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            partial.append(data);
            if (last) {
                try {
                    received.add(Json.mapper().readTree(partial.toString()));
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
                partial.setLength(0);
            }
            webSocket.request(1);
            return null;
        }
    }
}
