package server;

public class ServerMain {

    // di default ascolta solo sulla macchina stessa: da fuori si arriva attraverso Tailscale Funnel (o un proxy).
    // KOI_HOST=0.0.0.0 lo apre alla rete locale, utile per provare dal telefono di casa.
    // KOI_WEB_DIR=src/main/resources/web serve la pagina dai sorgenti: si modifica e si ricarica il browser
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        HistoryStore store = new HistoryStore(HistoryStore.defaultLocation());
        Lobby lobby = new Lobby(store);
        System.out.println("Storico partite: " + store.file() + " (" + lobby.history().size() + (lobby.history().size() == 1 ? " partita)" : " partite)"));

        String host = env("KOI_HOST", DEFAULT_HOST);
        int port = Integer.parseInt(env("KOI_PORT", String.valueOf(DEFAULT_PORT)));
        new WebServer(lobby).start(host, port, env("KOI_WEB_DIR", null));
        System.out.println("Server pronto su http://" + host + ":" + port);
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
