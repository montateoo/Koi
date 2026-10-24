package network;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PlayerSession extends Remote {
    void startGame() throws RemoteException;
    void drawCard(int position) throws RemoteException;
    void leave() throws RemoteException;
    void choose(int option) throws RemoteException;
    void placeWater(int q, int r) throws RemoteException;
    void placeBorder(int q, int r, int angle) throws RemoteException;
    void placeLantern(int q, int r) throws RemoteException;
    void placeLeafs(int q, int r) throws RemoteException;
    void placeLily(int q, int r) throws RemoteException;
    void placeKoi(int type, int q, int r, int angle) throws RemoteException;
    void moveKoi(int koiId, int q, int r, int angle) throws RemoteException;
    void endTurn() throws RemoteException;

}