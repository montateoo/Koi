package client;

import java.util.List;

import network.GameView;

public interface View {
    void showLobby(List<String> rooms);
    void showRoom(String roomId);
    void showMessage(String text);
    void showError(String text);
    void showGame(GameView view);
}
