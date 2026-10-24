package server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import game.Player;
import network.PlayerSession;

public class PlayerSessionImpl extends UnicastRemoteObject implements PlayerSession {

    private RoomController roomController;
    private Player player;


    protected PlayerSessionImpl(RoomController roomController, Player player) throws RemoteException {
        super();
        this.roomController = roomController;
        this.player = player;
    }

    @Override
    public void startGame() throws RemoteException {
        roomController.startGame(player);
    }

    @Override
    public void drawCard(int position) throws RemoteException {
        roomController.drawCard(player, position);
    }

    @Override
    public void leave() throws RemoteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'leave'");
    }

    @Override
    public void choose(int option) throws RemoteException {
        roomController.choose(player,option);
    }

    @Override
    public void placeWater(int q, int r) throws RemoteException {
        roomController.placeWater(player, q, r);
    }


    @Override
    public void placeBorder(int q, int r, int angle) throws RemoteException {
        roomController.placeBorder(player, q, r, angle);
    }

    @Override
    public void placeLantern(int q, int r) throws RemoteException {
        roomController.placeLantern(player, q, r);
    }

    @Override
    public void placeLeafs(int q, int r) throws RemoteException {
        roomController.placeLeafs(player, q, r);
    }

    @Override
    public void placeLily(int q, int r) throws RemoteException {
        roomController.placeLily(player, q, r);
    }

    @Override
    public void placeKoi(int type, int q, int r, int angle) throws RemoteException {
        roomController.placeKoi(player, type, q, r, angle);
    }

    @Override
    public void moveKoi(int koiId, int q, int r, int angle) throws RemoteException {
        roomController.moveKoi(player, koiId, q, r, angle);
    }

    @Override
    public void endTurn() throws RemoteException {
        roomController.endTurn(player);
    }
    
}
