package client;

import java.util.List;

import game.scoring.ScoreSheet;
import network.GameView;
import network.MatchRecord;

public interface View {
    void showLobby(List<String> rooms);
    void showRoom(String roomId);
    void showMessage(String text);
    void showError(String text);
    void showGame(GameView view);
    void showGameOver(List<ScoreSheet> ranking);   // classifica finale, dal primo all'ultimo
    void showHistory(List<MatchRecord> matches);   // partite finite, la piu' recente per prima
}
