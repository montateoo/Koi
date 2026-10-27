package game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class PondTest {

    // le due foglie del salice piangente accolgono la ninfea; l'albero e l'erba no
    @Test
    void lilyGoesOnTheWillowLeaves() {
        Pond pond = new Pond();
        HexPosition anchor = null;
        int rotation = -1;
        search:
        for (HexPosition candidate : pond.getPlayablePositions(TileType.THREE)) {
            for (int r : pond.validRotations(TileType.THREE, candidate)) {
                anchor = candidate;
                rotation = r;
                break search;
            }
        }
        assertTrue(anchor != null, "il salice deve trovare posto accanto alla sorgente");

        Tile willow = new Tile(TileType.THREE);
        pond.placeObjective(willow, anchor, rotation);
        List<HexPosition> piece = pond.positionsOf(willow);
        assertEquals(4, piece.size());

        Set<HexPosition> leaves = new HashSet<>();
        for (HexPosition position : piece) {
            if (pond.getCells().get(position).isFreeLeaf()) {
                leaves.add(position);
            }
        }
        assertEquals(2, leaves.size(), "acqua con le foglie: 2 esagoni su 4");
        assertTrue(pond.getPlayablePositions(TileType.LILY).containsAll(leaves));

        HexPosition leaf = leaves.iterator().next();
        pond.upgrade(leaf, new Tile(TileType.LILY));
        assertFalse(pond.getPlayablePositions(TileType.LILY).contains(leaf), "una ninfea per foglia");
    }

    // l'anteprima del client usa gli stessi esagoni che poi la tessera occupa davvero
    @Test
    void footprintMatchesWhatIsPlaced() {
        for (TileType type : List.of(TileType.THREE, TileType.STATUE, TileType.TURTLE)) {
            Pond pond = new Pond();
            HexPosition anchor = pond.getPlayablePositions(type).get(0);
            int rotation = pond.validRotations(type, anchor).get(0);
            List<HexPosition> preview = pond.footprint(type, anchor, rotation);
            Tile tile = new Tile(type);
            pond.placeObjective(tile, anchor, rotation);
            assertEquals(new HashSet<>(preview), new HashSet<>(pond.positionsOf(tile)), type.name());
        }
    }
}
