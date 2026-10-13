package game;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import game.cards.*;
import game.listeners.*;

public class Board {

    private ArrayList<Card> drawableCards;
    private ArrayList<Card> deckOfCards;
    final int boardSize = 5;
    private BoardListener listener;

    public Board(){
        drawableCards = new ArrayList<>(boardSize);
        deckOfCards = new ArrayList<>();
    }

    public void setListener(BoardListener listener) {
        this.listener = listener;
    }

    private void loadCards(int pNum){
       JSONArray cardData;
       JSONParser parser = new JSONParser();

        try {
            cardData = (JSONArray) parser.parse(new InputStreamReader(GameRoom.class.getResourceAsStream("/assets/cards.json"), StandardCharsets.UTF_8));
    
            for(Object o : cardData){
                JSONObject newCard = (JSONObject) o;

                int id = ((Number) newCard.get("id")).intValue();
                CardType cardType = CardType.valueOf((String) newCard.get("type"));
                int playerNum = ((Number) newCard.get("playerNum")).intValue();
                
                if(playerNum<=pNum){

                    switch (cardType){

                        case CardType.HELPER:

                            ArrayList<Actions> allowedActions = new ArrayList<>();
                            for (Object value : (JSONArray) newCard.get("allowedActions")) {
                            allowedActions.add(Enum.valueOf(Actions.class, (String) value));
                            }

                            deckOfCards.add(new HelperCard(id, cardType, playerNum, allowedActions));
                            break;

                        case CardType.WAREHOUSE:
                            deckOfCards.add(new WarehouseCard(id, cardType, playerNum, ((Number) newCard.get("additionalSpace")).intValue()));
                            break;

                        case CardType.MARKET:
                            deckOfCards.add(new MarketCard(id, cardType, playerNum, ((Number) newCard.get("discount")).intValue()));
                            break;

                        case CardType.RAIN:

                            ArrayList<TileType> rainTiles = new ArrayList<>();
                            for (Object value : (JSONArray) newCard.get("rainTiles")) {
                                rainTiles.add(Enum.valueOf(TileType.class, (String) value));
                            }

                            deckOfCards.add(new RainCard(id, cardType, playerNum, rainTiles));
                            break;

                        case CardType.WELL:
                            deckOfCards.add(new WellCard(id, cardType, playerNum, ((Number) newCard.get("carriedMoney")).intValue(), ((Number)newCard.get("carriedWater")).intValue(), ((Boolean)newCard.get("both"))));
                            break;

                        case CardType.SCROLL:
                            deckOfCards.add(new ScrollCard(id, cardType, playerNum, ((Number) newCard.get("pointsPerObject")).intValue(), Enum.valueOf( ScrollObjective.class , (String)newCard.get("objectiveObject"))));
                            break;

                        default:
                            deckOfCards.add(new Card(id, cardType, playerNum));
                    }
                }
            }
        }catch (FileNotFoundException e) {
        // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
        // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void shuffleCards(){
        Collections.shuffle(deckOfCards);
    }

    public void initializeBoard(int pNum){

        loadCards(pNum);
        shuffleCards();

        for(int i = 0; i<boardSize; i++){
            drawableCards.add(0,deckOfCards.removeFirst());
        }
    }

    public void updateBoard(){

        drawableCards.add(0,deckOfCards.removeFirst());      

    }

    public Card drawCard(int position){

        this.drawEffect(position);

        Card chosenCard = drawableCards.remove(position);

        this.updateBoard();

        return chosenCard;
    }


    private void drawEffect(int position) {
        if(position==0){
            listener.applyBoardEffect(position);
        }else if(position==2){
            listener.applyBoardEffect(position);
        }else if(position==3){
            listener.applyBoardEffect(position); 
        }else if(position==4){
            listener.applyBoardEffect(position); 
        }
    }

    public String toString(){

        String text = "";

        for(int i = 0; i<boardSize; i++){
            text = text + drawableCards.get(i).toString() + "|";
        }

        return text;
    }



}
