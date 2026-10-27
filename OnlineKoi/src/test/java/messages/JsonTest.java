package messages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import game.scoring.ScoreSheet;
import network.CellView;
import network.GameView;
import network.HexCoord;
import network.MatchRecord;
import network.ObjectiveView;
import network.PlacementView;
import network.PlayerView;
import network.RoomInfo;

class JsonTest {

    // ogni record di ClientMessage, anche quelli aggiunti in futuro: costruito con valori di prova,
    // scritto in JSON e riletto deve tornare uguale
    @Test
    void everyClientMessageSurvivesTheRoundTrip() throws Exception {
        for (Class<?> type : ClientMessage.class.getPermittedSubclasses()) {
            ClientMessage original = sample(type);
            String json = Json.mapper().writerFor(ClientMessage.class).writeValueAsString(original);
            assertTrue(json.contains("\"type\":\"" + type.getSimpleName() + "\""), json);
            assertEquals(original, Json.readClientMessage(json), json);
        }
    }

    // "type" in JSON e' il nome del messaggio: un componente con lo stesso nome lo sovrascriverebbe
    // (e' successo con PlaceKoi, che aveva "type" per la taglia della carpa)
    @Test
    void noMessageHasAFieldCalledType() {
        for (Class<?> base : List.of(ClientMessage.class, ServerMessage.class)) {
            for (Class<?> type : base.getPermittedSubclasses()) {
                for (RecordComponent component : type.getRecordComponents()) {
                    assertFalse(component.getName().equals("type"), type.getSimpleName() + " ha un campo \"type\"");
                }
            }
        }
    }

    @Test
    void readsWhatTheBrowserSends() throws Exception {
        assertEquals(new ClientMessage.PlaceKoi(1, 0, 1, 3),
                Json.readClientMessage("{\"type\":\"PlaceKoi\",\"size\":1,\"q\":0,\"r\":1,\"angle\":3}"));
        assertEquals(new ClientMessage.PlaceBorder(2, -1, 4),
                Json.readClientMessage("{\"type\":\"PlaceBorder\",\"q\":2,\"r\":-1,\"angle\":4}"));
        assertEquals(new ClientMessage.ListRooms(), Json.readClientMessage("{\"type\":\"ListRooms\"}"));
    }

    @Test
    void rejectsAnythingThatIsNotExactlyAClientMessage() {
        List<String> bad = List.of(
                "",
                "not json",
                "{\"q\":1,\"r\":2}",                                        // senza tipo
                "{\"type\":\"Shutdown\"}",                                  // tipo che non esiste
                "{\"type\":\"Info\",\"text\":\"ciao\"}",                    // messaggio del server, non del client
                "{\"type\":\"PlaceWater\",\"q\":1}",                        // campo mancante
                "{\"type\":\"PlaceWater\",\"q\":1,\"r\":null}",             // null al posto di un numero
                "{\"type\":\"PlaceWater\",\"q\":1,\"r\":2,\"extra\":3}",    // campo in piu'
                "{\"type\":\"PlaceWater\",\"q\":\"uno\",\"r\":2}",          // tipo sbagliato
                "{\"type\":\"ListRooms\"} {\"type\":\"ListRooms\"}");       // due messaggi in uno
        for (String json : bad) {
            assertThrows(JsonProcessingException.class, () -> Json.readClientMessage(json), json);
        }
    }

    @Test
    void gameViewSurvivesTheRoundTrip() throws Exception {
        GameView view = sampleView();
        String json = Json.write(new ServerMessage.GameUpdate(view));
        ServerMessage back = Json.mapper().readValue(json, ServerMessage.class);
        assertEquals(new ServerMessage.GameUpdate(view), back);

        JsonNode tree = Json.mapper().readTree(json);
        assertEquals("GameUpdate", tree.get("type").asText());
        // le rotazioni per ancoraggio hanno come chiave "q,r", che il JavaScript legge facilmente
        assertEquals(List.of(0, 3), List.of(tree.at("/view/rotations/BORDER/1,-1/0").asInt(), tree.at("/view/rotations/BORDER/1,-1/1").asInt()));
        assertFalse(tree.get("view").has("myTurn"), "solo i componenti del record diventano campi");
    }

    @Test
    void scoresCarryTheirTotal() throws Exception {
        ScoreSheet sheet = new ScoreSheet("Alice", 10, 4, 3, 7, 1, 5);
        String json = Json.write(new ServerMessage.GameOver(List.of(sheet)));
        assertEquals(25, Json.mapper().readTree(json).at("/ranking/0/total").asInt());
        assertEquals(new ServerMessage.GameOver(List.of(sheet)), Json.mapper().readValue(json, ServerMessage.class));
    }

    @Test
    void historySurvivesTheRoundTrip() throws Exception {
        PlayerView alice = sampleView().players().get(0);
        MatchRecord match = new MatchRecord("Stagno di prova", "27/10/2026 21:30",
                List.of(new ScoreSheet("Alice", 10, 4, 3, 7, 1, 5)), List.of(alice));
        ServerMessage history = new ServerMessage.History(List.of(match));
        assertEquals(history, Json.mapper().readValue(Json.write(history), ServerMessage.class));
    }

    // ---------------------------------------------------------------- dati di prova

    private static ClientMessage sample(Class<?> type) throws Exception {
        RecordComponent[] components = type.getRecordComponents();
        Class<?>[] types = new Class<?>[components.length];
        Object[] values = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            types[i] = components[i].getType();
            values[i] = types[i] == int.class ? i + 1 : components[i].getName() + "-prova";
        }
        return (ClientMessage) type.getDeclaredConstructor(types).newInstance(values);
    }

    private static GameView sampleView() {
        CellView source = new CellView(0, 0, "WATERSOURCE", "SMALLKOI", 0, 1, 7, List.of(true, true, true, true, true, true));
        CellView border = new CellView(1, -1, "BORDER", null, 0, 2, -1, List.of(true, false, false, true, true, true));
        PlayerView alice = new PlayerView("Alice", 3, 1, 2, 6, List.of("WATER", "BORDER"),
                List.of("Aiutante: piazza fino a 2 acque | usi: 1/2"), List.of(), List.of("Pergamena: 2 punti per ogni lanterna"),
                List.of(source, border));
        PlayerView bob = new PlayerView("Bob", 5, 0, 0, 6, List.of(), List.of(), List.of(), List.of(), List.of());
        return new GameView("Alice", "Alice", List.of("Pozzo: 1 moneta e 2 acqua", ""), 4, 3, 2, List.of(alice, bob),
                "Quale carpa compri?", List.of("Carpa piccola", "Non compro"),
                Map.of("BORDER", List.of(new HexCoord(1, -1)), GameView.moveKey(7), List.of(new HexCoord(0, 1))),
                Map.of("BORDER", Map.of(new HexCoord(1, -1), List.of(0, 3))),
                true,
                List.of(new ObjectiveView("TURTLE", "Tartaruga", "una carpa circondata", 2, 4, "TURTLE", false)),
                List.of(new PlacementView(0, new HexCoord(1, 0), 7, List.of(new HexCoord(1, 0), new HexCoord(2, 0)))));
    }

    @Test
    void roomListKeepsNamesAndPlayers() throws Exception {
        ServerMessage rooms = new ServerMessage.RoomList(List.of(new RoomInfo("0", "Stagno", List.of("Alice"), true)));
        assertEquals(rooms, Json.mapper().readValue(Json.write(rooms), ServerMessage.class));
    }
}
