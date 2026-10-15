package server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import network.ClientCallback;
import network.Lobby;
import network.PlayerSession;

import game.Player;

public class LobbyImpl extends UnicastRemoteObject implements Lobby {

	private static AtomicLong idCounter = new AtomicLong();
    private ConcurrentHashMap<String,RoomController> activeGames;

	public LobbyImpl() throws RemoteException{
		activeGames = new ConcurrentHashMap<>();
	}

	@Override
	public String createRoom(String roomName) throws RemoteException {
		String gameRoomId = String.valueOf(createID());
		RoomController newGameRoom = new RoomController(gameRoomId,roomName);
		activeGames.put(gameRoomId,newGameRoom);
		return gameRoomId;
	}

	@Override
	public List<String> listRooms() throws RemoteException {
		return Collections.list(activeGames.keys());
	}

	public static long createID()
	{
		return idCounter.getAndIncrement();
	}

	@Override
	public PlayerSession join(String roomId, String nickname, ClientCallback client) throws RemoteException {
		RoomController chosenRoom = activeGames.get(roomId); //TODO gestire se la stanza non esiste
		Player newPlayer = new Player(nickname);
		chosenRoom.addPlayer(newPlayer,client); //TODO gestire se non è possibile entrare nella stanza
		return new PlayerSessionImpl(chosenRoom,newPlayer);
	}

    
}
