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

    private ArrayList<KoiCarp> smallKoiCarps;
    private ArrayList<KoiCarp> mediumKoiCarps;
    private ArrayList<KoiCarp> bigKoiCarps;

    public KoiMarket(){
        smallKoiCarps = new ArrayList<>();
        mediumKoiCarps = new ArrayList<>();
        bigKoiCarps = new ArrayList<>();
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

                    case TileType.BIGKOI:
                        bigKoiCarps.add(new KoiCarp(id, carpType));
                        break;

                    case TileType.MEDIUMKOI:
                        mediumKoiCarps.add(new KoiCarp(id, carpType));
                        break;

                    case TileType.SMALLKOI:
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

    public Tile takeSmallKoi() {
        return smallKoiCarps.removeFirst();
    }

    public Tile takeMediumKoi() {
       return mediumKoiCarps.removeFirst();
    }

    public Tile takeBigKoi() {
        return bigKoiCarps.removeFirst();
    }

}
