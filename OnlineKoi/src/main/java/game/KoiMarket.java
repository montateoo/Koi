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

public class KoiMarket {

    private final int smallKoiCost = 1;
    private final int mediumKoiCost = 2;
    private final int bigKoiCost = 3;
    private GameRoom listener;

    private ArrayList<KoiCarp> smallKoiCarps;
    private ArrayList<KoiCarp> mediumKoiCarps;
    private ArrayList<KoiCarp> bigKoiCarps;
    
    public KoiMarket(){
        smallKoiCarps = new ArrayList<>();
        mediumKoiCarps = new ArrayList<>();
        bigKoiCarps = new ArrayList<>();

    }

    public int getSmallKoiCost() {
        return smallKoiCost;
    }

    public int getMediumKoiCost() {
        return mediumKoiCost;
    }

    public int getBigKoiCost() {
        return bigKoiCost;
    }

    private void loadKoiCarps(){
       JSONArray KoiCarpsData;
       JSONParser parser = new JSONParser();

        try {
            KoiCarpsData = (JSONArray) parser.parse(new InputStreamReader(KoiMarket.class.getResourceAsStream("/assets/koiCarps.json"), StandardCharsets.UTF_8));
    
            for(Object o : KoiCarpsData){
                JSONObject newCarp = (JSONObject) o;

                int id = ((Number) newCarp.get("id")).intValue();
                TileType carpType = TileType.valueOf((String) newCarp.get("type"));

                switch (carpType){

                    case BIGKOI:
                        bigKoiCarps.add(new KoiCarp(id, carpType));
                        break;

                    case MEDIUMKOI:
                        mediumKoiCarps.add(new KoiCarp(id, carpType));
                        break;

                    case SMALLKOI:
                        smallKoiCarps.add(new KoiCarp(id, carpType));
                        break;

                    default:
                
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

    private void shuffleKoiCarps(){
        Collections.shuffle(smallKoiCarps);
        Collections.shuffle(mediumKoiCarps);
        Collections.shuffle(bigKoiCarps);

    }

    public void initializeMarket(int size){

        loadKoiCarps();
        shuffleKoiCarps();

        if(size==2){
            smallKoiCarps.removeLast();

            mediumKoiCarps.removeLast();
            mediumKoiCarps.removeLast();

            bigKoiCarps.removeLast();
            bigKoiCarps.removeLast();

        }else if(size==3){
            smallKoiCarps.removeLast();

            mediumKoiCarps.removeLast();

            bigKoiCarps.removeLast();
            bigKoiCarps.removeLast();
        }
        
    }

    public int getSmallKoiLeft() {
        return smallKoiCarps.size();
    }

    public int getMediumKoiLeft() {
        return mediumKoiCarps.size();
    }

    public int getBigKoiLeft() {
        return bigKoiCarps.size();
    }

    public Tile takeSmallKoi() {
        Tile koi = smallKoiCarps.removeFirst();
        checkEmpty();
        return koi;
    }

    public Tile takeMediumKoi() {
        Tile koi = mediumKoiCarps.removeFirst();
        checkEmpty();
        return koi;
    }

    public Tile takeBigKoi() {
        Tile koi = bigKoiCarps.removeFirst();
        checkEmpty();
        return koi;
    }

    // carpa scartata dal magazzino a fine turno: torna sul mercato
    public void giveBack(Tile koi) {
        switch (koi.getTileType()) {
            case SMALLKOI:
                smallKoiCarps.add((KoiCarp) koi);
                break;
            case MEDIUMKOI:
                mediumKoiCarps.add((KoiCarp) koi);
                break;
            case BIGKOI:
                bigKoiCarps.add((KoiCarp) koi);
                break;
            default:
                throw new IllegalArgumentException(koi.getTileType() + " is not a koi");
        }
    }

    // regolamento pag. 11: la fine scatta quando viene presa l'ultima carpa della riserva
    private void checkEmpty() {
        if (smallKoiCarps.isEmpty() && mediumKoiCarps.isEmpty() && bigKoiCarps.isEmpty()) {
            listener.noMoreKoi();
        }
    }

	public void setListener(GameRoom gameRoom) {
		listener = gameRoom;
	}

}
