package network;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientCallback extends Remote {
    void onGameUpdate(GameView view) throws RemoteException;
    void onMessage(String text) throws RemoteException;
}
