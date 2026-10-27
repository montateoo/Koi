package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import messages.SerialFilters;
import network.MatchRecord;

// lo storico delle partite su disco, cosi' sopravvive ai riavvii del server.
// Posizione: $KOI_HISTORY se impostata, altrimenti la cartella dati dell'utente (standard XDG su Linux):
// $XDG_DATA_HOME/koi/history.dat oppure ~/.local/share/koi/history.dat
final class HistoryStore {

    private final Path file;

    HistoryStore(Path file) {
        this.file = file;
    }

    static Path defaultLocation() {
        String custom = System.getenv("KOI_HISTORY");
        if (custom != null && !custom.isBlank()) {
            return Path.of(custom);
        }
        String xdg = System.getenv("XDG_DATA_HOME");
        Path dataDir = (xdg != null && !xdg.isBlank())
                ? Path.of(xdg)
                : Path.of(System.getProperty("user.home"), ".local", "share");
        return dataDir.resolve("koi").resolve("history.dat");
    }

    Path file() {
        return file;
    }

    // all'avvio. Un file illeggibile (rovinato, o di una versione vecchia del gioco) viene messo da parte
    // come .bak e si riparte da uno storico vuoto: il server deve partire comunque
    @SuppressWarnings("unchecked")
    List<MatchRecord> load() {
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            in.setObjectInputFilter(SerialFilters.FROM_SERVER);   // le stesse classi che il client accetta dal server
            return new ArrayList<>((List<MatchRecord>) in.readObject());
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            System.err.println("Storico illeggibile (" + e + "): lo metto da parte e riparto da zero");
            try {
                Files.move(file, file.resolveSibling(file.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                // se non si riesce a spostarlo verra' sovrascritto al prossimo salvataggio
            }
            return new ArrayList<>();
        }
    }

    // a ogni partita finita: scrive su un file temporaneo e poi lo sostituisce in un colpo solo,
    // cosi' uno spegnimento a meta' scrittura non rovina lo storico gia' salvato
    synchronized void save(List<MatchRecord> matches) {
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(temp))) {
                out.writeObject(List.copyOf(matches));
            }
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            System.err.println("Non riesco a salvare lo storico in " + file + ": " + e.getMessage());
        }
    }
}
