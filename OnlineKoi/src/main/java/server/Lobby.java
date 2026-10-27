package server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import network.MatchRecord;
import network.RoomInfo;

public class Lobby {

	private static AtomicLong idCounter = new AtomicLong();
    private ConcurrentHashMap<String,RoomController> activeGames;
    // partite finite, la piu' recente per prima; salvate su disco a ogni partita finita
    private final List<MatchRecord> history;
    private final HistoryStore store;

	public Lobby(HistoryStore store) {
		activeGames = new ConcurrentHashMap<>();
		this.store = store;
		this.history = new CopyOnWriteArrayList<>(store.load());
	}

	public String createRoom(String roomName) {
		String gameRoomId = String.valueOf(createID());
		// a fine partita la stanza si toglie dalla lista e la partita passa nello storico;
		// se restano senza nessuno (tutti usciti o scollegati) si toglie e basta
		RoomController newGameRoom = new RoomController(gameRoomId, roomName, match -> finishRoom(gameRoomId, match),
				() -> activeGames.remove(gameRoomId));
		activeGames.put(gameRoomId,newGameRoom);
		return gameRoomId;
	}

	// le stanze in ordine di creazione, con nome e giocatori
	public List<RoomInfo> listRooms() {
		List<RoomInfo> rooms = new ArrayList<>();
		for (RoomController room : activeGames.values()) {
			rooms.add(room.info());
		}
		rooms.sort(Comparator.comparingLong(room -> Long.parseLong(room.id())));
		return rooms;
	}

	private static long createID()
	{
		return idCounter.getAndIncrement();
	}

	public RoomController getRoom(String roomId) {
		return activeGames.get(roomId);
	}

	private void finishRoom(String roomId, MatchRecord match) {
		activeGames.remove(roomId);
		history.add(0, match);
		store.save(history);
	}

	public List<MatchRecord> history() {
		return List.copyOf(history);
	}

}
