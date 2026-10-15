package network;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface Lobby extends Remote {
    String createRoom(String roomName) throws RemoteException;
    List<String> listRooms() throws RemoteException;
    PlayerSession join(String roomId, String nickname, ClientCallback client) throws RemoteException;
}
