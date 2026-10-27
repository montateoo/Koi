package messages;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;

import game.scoring.ScoreSheet;
import network.HexCoord;

// conversione da e verso JSON di tutto cio' che attraversa la rete (e dello storico su disco).
// Ogni messaggio ha il nome del suo record nel campo "type":
//   {"type":"PlaceWater","q":1,"r":-2}      {"type":"Info","text":"Tocca a Bob"}
// L'elenco dei tipi viene dalle interfacce sealed: un record nuovo in ClientMessage o ServerMessage e' subito
// riconosciuto, senza annotazioni da tenere allineate. Dal client si accettano solo quei record, con tutti i campi
// e nessun campo in piu': qualunque altra cosa e' un errore, non un oggetto costruito a caso
public final class Json {

    private static final ObjectMapper MAPPER = createMapper();
    private static final ObjectReader CLIENT_READER = MAPPER.readerFor(ClientMessage.class);
    private static final ObjectWriter SERVER_WRITER = MAPPER.writerFor(ServerMessage.class);

    private Json() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static String write(ServerMessage message) {
        try {
            return SERVER_WRITER.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Messaggio non convertibile in JSON: " + message, e);
        }
    }

    public static ClientMessage readClientMessage(String json) throws JsonProcessingException {
        return CLIENT_READER.readValue(json);
    }

    // ---------------------------------------------------------------- configurazione

    // il campo "type" con il nome semplice del record ("PlaceWater"), messo sulle interfacce senza toccarle
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    private interface Typed {
    }

    // il totale non e' un componente del record ma al client serve: si scrive, e rileggendo si ignora
    private abstract static class ScoreSheetTotal {
        @JsonProperty(value = "total", access = JsonProperty.Access.READ_ONLY)
        abstract int total();
    }

    private static ObjectMapper createMapper() {
        SimpleModule coordinates = new SimpleModule("coordinate");
        // HexCoord come chiave di una mappa (rotazioni per ancoraggio): "q,r"
        coordinates.addKeySerializer(HexCoord.class, new JsonSerializer<HexCoord>() {
            @Override
            public void serialize(HexCoord coord, JsonGenerator out, SerializerProvider provider) throws IOException {
                out.writeFieldName(coord.q() + "," + coord.r());
            }
        });
        coordinates.addKeyDeserializer(HexCoord.class, new KeyDeserializer() {
            @Override
            public Object deserializeKey(String key, DeserializationContext context) throws IOException {
                String[] parts = key.split(",");
                if (parts.length != 2) {
                    return context.handleWeirdKey(HexCoord.class, key, "atteso \"q,r\"");
                }
                try {
                    return new HexCoord(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
                } catch (NumberFormatException e) {
                    return context.handleWeirdKey(HexCoord.class, key, "atteso \"q,r\"");
                }
            }
        });

        JsonMapper mapper = JsonMapper.builder()
                .addModule(coordinates)
                .addMixIn(ClientMessage.class, Typed.class)
                .addMixIn(ServerMessage.class, Typed.class)
                .addMixIn(ScoreSheet.class, ScoreSheetTotal.class)
                // solo i componenti dei record: metodi come GameView.isMyTurn() non diventano campi
                .visibility(PropertyAccessor.IS_GETTER, Visibility.NONE)
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .build();
        for (Class<?> type : ClientMessage.class.getPermittedSubclasses()) {
            mapper.registerSubtypes(new NamedType(type, type.getSimpleName()));
        }
        for (Class<?> type : ServerMessage.class.getPermittedSubclasses()) {
            mapper.registerSubtypes(new NamedType(type, type.getSimpleName()));
        }
        return mapper;
    }
}
