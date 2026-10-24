package server;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class Lobby {

	private static AtomicLong idCounter = new AtomicLong();
    private ConcurrentHashMap<String,RoomController> activeGames;

	public Lobby() {
		activeGames = new ConcurrentHashMap<>();
	}

	public String createRoom(String roomName) {
		String gameRoomId = String.valueOf(createID());
		RoomController newGameRoom = new RoomController(gameRoomId,roomName);
		activeGames.put(gameRoomId,newGameRoom);
		return gameRoomId;
	}

	public List<String> listRooms() {
		return Collections.list(activeGames.keys());
	}

	private static long createID()
	{
		return idCounter.getAndIncrement();
	}

	public RoomController getRoom(String roomId) {
		return activeGames.get(roomId);
	}
    
}
