package game;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    final int firstCardCost = 1;
    final int thirdBonusMoney = 2;
    final int thirdBonusWater = 1;
    final int fourthBonusMoney = 1;
    final int fourthBonusWater = 2;
    final int fifthBonusBorder = 1;
    final int fifthBonusLeaf = 1;
    private BoardListener listener;

    public Board(){
        drawableCards = new ArrayList<>(boardSize);
        deckOfCards = new ArrayList<>();
    }

    public void setListener(BoardListener listener) {
        this.listener = listener;
    }

    public List<Card> getDrawableCards() {
        return Collections.unmodifiableList(drawableCards);
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

                        case HELPER:

                            ArrayList<Actions> allowedActions = new ArrayList<>();
                            for (Object value : (JSONArray) newCard.get("allowedActions")) {
                                allowedActions.add(Enum.valueOf(Actions.class, (String) value));
                            }
                            int rawCapacity = ((Number) newCard.get("capacity")).intValue();

                            deckOfCards.add(new HelperCard(id, cardType, playerNum, allowedActions, rawCapacity));
                            break;

                        case WAREHOUSE:
                            deckOfCards.add(new WarehouseCard(id, cardType, playerNum, ((Number) newCard.get("additionalSpace")).intValue()));
                            break;

                        case MARKET:
                            deckOfCards.add(new MarketCard(id, cardType, playerNum, ((Number) newCard.get("discount")).intValue()));
                            break;

                        case RAIN:

                            ArrayList<TileType> rainTiles = new ArrayList<>();
                            for (Object value : (JSONArray) newCard.get("rainTiles")) {
                                rainTiles.add(Enum.valueOf(TileType.class, (String) value));
                            }

                            deckOfCards.add(new RainCard(id, cardType, playerNum, rainTiles));
                            break;

                        case WELL:
                            deckOfCards.add(new WellCard(id, cardType, playerNum, ((Number) newCard.get("carriedMoney")).intValue(), ((Number)newCard.get("carriedWater")).intValue(), ((Boolean)newCard.get("both"))));
                            break;

                        case SCROLL:
                            deckOfCards.add(new ScrollCard(id, cardType, playerNum, ((Number) newCard.get("pointsPerObject")).intValue(), Enum.valueOf( ScrollObjective.class , (String)newCard.get("objectiveObject"))));
                            break;

                        default:
                            deckOfCards.add(new Card(id, cardType, playerNum));
                    }
                }
            }
        }catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
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
            listener.playerHasToPay(firstCardCost);
        }else if(position==2){
            listener.playerGiveBonusMoneyWater(thirdBonusMoney,thirdBonusWater);
        }else if(position==3){
            listener.playerAskBonusOrMarket(fourthBonusMoney,fourthBonusWater);
        }else if(position==4){
            listener.playerAskBorderOrLeaf(fifthBonusBorder,fifthBonusLeaf); 
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
