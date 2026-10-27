package server;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;

import game.TileType;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.websocket.WsContext;
import messages.ClientMessage;
import messages.Json;
import messages.ServerMessage;

// il trasporto: Javalin (Jetty) serve la pagina del gioco da resources/web e i WebSocket su /ws.
// Ogni WebSocket ha il suo ClientHandler; i messaggi viaggiano in JSON (vedi Json)
public final class WebServer {

    // un messaggio del client e' un record con pochi numeri e stringhe: oltre questa misura non e' un client vero
    private static final int MAX_MESSAGE = 4 * 1024;
    // un giocatore puo' restare fermo a lungo mentre gli altri giocano: i ping tengono viva la connessione
    // (anche attraverso proxy come Tailscale Funnel), il timeout chiude solo le connessioni davvero morte
    private static final Duration IDLE_TIMEOUT = Duration.ofMinutes(2);
    private static final long PING_SECONDS = 20;

    private final Lobby lobby;
    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private final ServerMessage.Welcome welcome = new ServerMessage.Welcome(tileNames());

    public WebServer(Lobby lobby) {
        this.lobby = lobby;
    }

    // webDir: cartella da cui servire la pagina (sviluppo: si modifica e si ricarica il browser);
    // null = la copia dentro il jar
    public Javalin start(String host, int port, String webDir) {
        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            if (webDir == null) {
                config.staticFiles.add("/web", Location.CLASSPATH);
            } else {
                config.staticFiles.add(webDir, Location.EXTERNAL);
            }
            config.jetty.modifyWebSocketServletFactory(factory -> {
                factory.setMaxTextMessageSize(MAX_MESSAGE);
                factory.setIdleTimeout(IDLE_TIMEOUT);
            });
        });
        app.ws("/ws", ws -> {
            ws.onConnect(ctx -> {
                ctx.enableAutomaticPings(PING_SECONDS, TimeUnit.SECONDS);
                ClientHandler handler = new ClientHandler(lobby, new WsConnection(ctx));
                clients.put(ctx.sessionId(), handler);
                handler.send(welcome);
            });
            ws.onMessage(ctx -> {
                ClientHandler handler = clients.get(ctx.sessionId());
                if (handler == null) {
                    return;
                }
                ClientMessage message;
                try {
                    message = Json.readClientMessage(ctx.message());
                } catch (JsonProcessingException e) {
                    handler.send(new ServerMessage.ErrorMessage("Messaggio non valido"));
                    return;
                }
                handler.handle(message);
            });
            ws.onClose(ctx -> {
                ClientHandler handler = clients.remove(ctx.sessionId());
                if (handler != null) {
                    handler.disconnected();
                }
            });
        });
        return app.start(host, port);
    }

    public Javalin start(String host, int port) {
        return start(host, port, null);
    }

    private static Map<String, String> tileNames() {
        Map<String, String> names = new LinkedHashMap<>();
        for (TileType type : TileType.values()) {
            names.put(type.name(), type.displayName());
        }
        return names;
    }

    // un WebSocket visto come Connection. Gli invii arrivano da piu' thread (la stanza scrive a tutti i giocatori
    // mentre ciascuno puo' ricevere la propria risposta): uno alla volta per connessione
    private static final class WsConnection implements Connection {

        private final WsContext ctx;

        WsConnection(WsContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public synchronized void send(ServerMessage message) {
            if (!ctx.session.isOpen()) {
                return;
            }
            try {
                ctx.send(Json.write(message));
            } catch (RuntimeException e) {
                ctx.closeSession();   // client sparito a meta': onClose fara' il resto
            }
        }
    }
}
