package mindustry.world.blocks.environment;

import arc.graphics.g2d.TextureRegion;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FloorEdgeTest {

    @Test
    void testEdgeBounds() {
        if (Vars.content == null) {
            Vars.content = new ContentLoader();
        }

        Floor floor = new Floor("test-floor") {
            @Override
            public void load() {
            }

            @Override
            protected TextureRegion[][] edges(int x, int y) {
                TextureRegion[][] regions = new TextureRegion[2][2];
                for(int i = 0; i < 2; i++) {
                    for(int j = 0; j < 2; j++) {
                        regions[i][j] = new TextureRegion();
                    }
                }
                return regions;
            }
        };

        // Should not throw exception
        assertDoesNotThrow(() -> floor.edge(0, 0, 0, 2));
        assertDoesNotThrow(() -> floor.edge(0, 0, 1, 1));

        // These will fail before fix
        assertDoesNotThrow(() -> floor.edge(0, 0, 2, 0));
        assertDoesNotThrow(() -> floor.edge(0, 0, 0, 0));
        assertDoesNotThrow(() -> floor.edge(0, 0, 2, 2));
    }
}
