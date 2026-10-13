package game;

public class Main {

    public static void main(String[] args){

        GameRoom room = new GameRoom();
        Board board = new Board();

        Player p1 = new Player();
        Player p2 = new Player();
        Player p3 = new Player();
        Player p4 = new Player();

        room.addPlayer(p1);
        room.addPlayer(p2);
        room.addPlayer(p3);
        room.addPlayer(p4);

        System.out.println("Game startin...");

        room.prepareGame();
        board = room.getBoard();

        System.out.println(board.toString());

        System.out.println("now draw Card n: 3");

        board.drawCard(3);

        System.out.println(board.toString());



    }
}
