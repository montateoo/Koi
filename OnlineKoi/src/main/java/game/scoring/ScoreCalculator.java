package game.scoring;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import game.Cell;
import game.HexPosition;
import game.KoiCarp;
import game.Player;
import game.Pond;
import game.Tile;
import game.TileType;
import game.cards.CardType;
import game.objectives.ObjectiveType;
import game.cards.ScrollCard;

// conteggio di fine partita (regolamento pag. 11 e 13): legge lo stagno e le carte del giocatore, non cambia niente
public final class ScoreCalculator {

    public ScoreSheet score(Player player) {
        Pond pond = player.getPond();
        int koi = koiPoints(pond);              // taglia, ninfee e ninfee viola vicine, raddoppio se circondata
        int lanterns = lanternPoints(pond);     // 1 per esagono in linea retta davanti
        int scrolls = scrollPoints(player);     // dalle carte Pergamena
        int objectives = objectivePoints(player,pond); // rami, cascata, statua, ponticello, ...
        int fortune = player.getInventory().getLuckyCards();
        return new ScoreSheet(player.getName(), koi, lanterns, scrolls, objectives, fortune,
                player.getInventory().getCoins());   // le monete servono solo per lo spareggio
    }

    private int scrollPoints(Player player) {
        List<ScrollCard> scrolls = player.getScrolls();
        int scrollPoints = 0;

        for(ScrollCard s:scrolls){
            switch(s.getScrollObjective()){

                case BIGKOI:
                    scrollPoints = scrollPoints + s.getPointsPerObject() * player.countTops(TileType.BIGKOI);
                    break;
                case MEDIUMKOI:
                    scrollPoints = scrollPoints + s.getPointsPerObject() * player.countTops(TileType.MEDIUMKOI);
                    break;
                case SMALLKOI:
                    scrollPoints = scrollPoints + s.getPointsPerObject() * player.countTops(TileType.SMALLKOI);
                    break;
                case LANTERN:
                    scrollPoints = scrollPoints + s.getPointsPerObject() * player.countTops(TileType.LANTERN);
                    break;
                case LILY:
                    scrollPoints = scrollPoints + s.getPointsPerObject() * player.countTops(TileType.LILY);
                    break;
                case WELL:
                    scrollPoints = scrollPoints + s.getPointsPerObject() * player.countCards(CardType.WELL);
                    break;
                case MARKET:
                    scrollPoints = scrollPoints + s.getPointsPerObject() * player.countCards(CardType.MARKET);
                    break;
                case WAREHOUSE:
                    scrollPoints = scrollPoints + s.getPointsPerObject() * player.countCards(CardType.WAREHOUSE);
                    break;
            }
        }

        return scrollPoints;
    }

    // regolamento pag. 11: ogni lanterna da' 1 punto per ogni esagono del laghetto (libero o occupato, di qualunque
    // tessera) in linea retta davanti a se', cioe' dal lato azzurro della sua sponda; i buchi non interrompono la linea
    private int lanternPoints(Pond pond) {
        int points = 0;
        for (Map.Entry<HexPosition, Cell> entry : pond.getCells().entrySet()) {
            Tile top = entry.getValue().getTop();
            if (top != null && top.getTileType() == TileType.LANTERN) {
                points += pond.countInLine(entry.getKey(), pond.facing(entry.getKey()));
            }
        }
        return points;
    }

    // regolamento pag. 11: valore per taglia (2/4/6), +1 per ogni ninfea adiacente e +2 per ogni ninfea viola
    // adiacente (pag. 13), il tutto raddoppiato se la carpa e' completamente circondata da lati azzurri.
    // La carpa centenaria non e' qui: si conta con gli obiettivi, ma con lo stesso koiValue (base 8)
    private int koiPoints(Pond pond) {
        int points = 0;
        for (List<HexPosition> koi : pond.topPieces(TileType.SMALLKOI, TileType.MEDIUMKOI, TileType.BIGKOI).values()) {
            int base = KoiCarp.pointsFor(pond.getCells().get(koi.get(0)).getTop().getTileType());
            points += koiValue(pond, koi, base);
        }
        return points;
    }

    // valore di una carpa che occupa questi esagoni, partendo dal suo valore base
    private int koiValue(Pond pond, List<HexPosition> koi, int base) {
        int value = base;
        for (HexPosition next : pond.neighboursOf(koi)) {
            Tile top = pond.getCells().get(next).getTop();
            if (top == null) {
                continue;
            }
            if (top.getTileType() == TileType.LILY) {
                value += 1;
            } else if (top.getTileType() == TileType.PURPLELILY) {
                value += ObjectiveType.PURPLELILY.points();
            }
        }
        return pond.surroundedByBlue(koi) ? value * 2 : value;
    }

    // regolamento pag. 13. Due famiglie di tessere obiettivo: quelle sopra l'acqua (topPieces) e quelle che sono
    // pezzi di laghetto (basePieces). Ogni tessera si conta una volta, con tutti i suoi esagoni in "piece"
    private int objectivePoints(Player player, Pond pond) {

        int points = 0;
        List<Tile> claimedObjective = player.getObjectiveTiles();

        for(Tile objective: claimedObjective){
            List<HexPosition> piece = pond.positionsOf(objective);   // gli esagoni della tessera nello stagno

            switch (objective.getTileType()) {
                case SMALLBRANCH:
                    points = points + ObjectiveType.BRANCHES.points(0);
                    break;
                case MEDIUMBRANCH:
                    points = points + ObjectiveType.BRANCHES.points(1);
                    break;
                case BIGBRANCH:
                    points = points + ObjectiveType.BRANCHES.points(2);
                    break;
                case THREE:
                    points = points + ObjectiveType.THREE.points(0);
                    break;
                case WATERFALL:
                    if(pond.waterSourceFilled()){
                        points = points + ObjectiveType.WATERFALL.points(1);
                    } else {
                        points = points + ObjectiveType.WATERFALL.points(0);
                    }
                    break;
                case BRIDGE: {
                    int bridgeDirection = -1;
                    for (HexPosition hex : piece) {
                        if (pond.facing(hex) != -1) {
                            bridgeDirection = pond.facing(hex);
                            break;
                        }
                    }
                    for (HexPosition hex : piece) {
                        points = points + ObjectiveType.BRIDGE.points() * pond.countInLine(hex, bridgeDirection);
                    }
                    break;
                }
                case STATUE: {
                    HexPosition statueCenter = null;
                    int statueDirection = -1;
                    for (HexPosition hex : piece) {
                        if (pond.facing(hex) != -1) {
                            statueCenter = hex;
                            statueDirection = pond.facing(hex);
                            break;
                        }
                    }
                    Set<Integer> koiOnLine = new HashSet<>();
                    if (statueCenter != null) {
                        for (HexPosition hex : pond.lineFrom(statueCenter, statueDirection)) {
                            Tile top = pond.getCells().get(hex).getTop();
                            if (top != null && (top.getTileType().isKoi() || top.getTileType() == TileType.OLDCARP)) {
                                koiOnLine.add(top.getTileID());
                            }
                        }
                    }
                    points = points + ObjectiveType.STATUE.points() * koiOnLine.size();
                    break;
                }
                case SHISHIODOSHI: {
                    Set<Integer> koiAround = new HashSet<>();
                    for (HexPosition hex : pond.neighboursOf(piece)) {
                        Tile top = pond.getCells().get(hex).getTop();
                        if (top != null && (top.getTileType().isKoi() || top.getTileType() == TileType.OLDCARP)) {
                            koiAround.add(top.getTileID());
                        }
                    }
                    points = points + ObjectiveType.SHISHIODOSHI.points() * koiAround.size();
                    break;
                }
                case TURTLE: {
                    int freeWater = 0;
                    for (HexPosition hex : pond.neighboursOf(piece)) {
                        if (pond.getCells().get(hex).isFreeWater()) {
                            freeWater++;
                        }
                    }
                    points = points + ObjectiveType.TURTLE.points(freeWater);
                    break;
                }
                case OLDCARP:
                    if (!piece.isEmpty()) {
                        points = points + koiValue(pond, piece, ObjectiveType.OLDCARP.points());
                    }
                    break;
                case PURPLELILY:
                    break;   // non segna da sola: aumenta le carpe vicine, gia' contata in koiPoints
                case PATH:
                    break;   // vale per numero di tessere, non per tessera: si conta una volta sola dopo il ciclo
                default:
                    break;
            }
        }

        points = points + pathPoints(pond);   // 3/6/12/18 per tutto il percorso (0 se non ce l'ha)
        return points;
    }

    private int pathPoints(Pond pond) {
        return ObjectiveType.PATH.points(pond.pathLength());
    }

}
