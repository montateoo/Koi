package client.gui;

import java.util.prefs.Preferences;

// impostazioni del giocatore che restano tra un avvio e l'altro (salvate nelle preferenze dell'utente del sistema)
final class Settings {

    private static final Preferences PREFS = Preferences.userNodeForPackage(Settings.class);

    private static final String NICKNAME = "nickname";
    private static final String HOST = "serverHost";
    private static final String PORT = "serverPort";
    private static final String SKIP_INTRO = "skipIntro";

    static final String DEFAULT_HOST = "localhost";
    static final int DEFAULT_PORT = 1099;

    private Settings() {
    }

    static String nickname() {
        return PREFS.get(NICKNAME, "");
    }

    static void setNickname(String nickname) {
        PREFS.put(NICKNAME, nickname.trim());
    }

    static boolean hasNickname() {
        return !nickname().isEmpty();
    }

    static String host() {
        return PREFS.get(HOST, DEFAULT_HOST);
    }

    static int port() {
        return PREFS.getInt(PORT, DEFAULT_PORT);
    }

    static void setServer(String host, int port) {
        PREFS.put(HOST, host.trim());
        PREFS.putInt(PORT, port);
    }

    // salta l'animazione d'apertura (nero che svanisce e zoom) e mostra subito il menu
    static boolean skipIntro() {
        return PREFS.getBoolean(SKIP_INTRO, false);
    }

    static void setSkipIntro(boolean skip) {
        PREFS.putBoolean(SKIP_INTRO, skip);
    }
}
