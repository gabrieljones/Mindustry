package mindustry.world;

import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

public class Edges{
    private static final int maxRadius = 12;
    private static Vec2[][] polygons = new Vec2[maxRadius * 2][0];

    static{
        for(int i = 0; i < maxRadius * 2; i++){
            polygons[i] = Geometry.pixelCircle((i + 1) / 2f);
        }
    }

    public static void iterateEdges(int size, int x, int y, Intc2 consumer){
        Hex.getRing(x, y, size, consumer);
    }

    public static void iterateInsideEdges(int size, int x, int y, Intc2 consumer){
        if(size <= 1){
            consumer.get(x, y);
        }else{
            Hex.getRing(x, y, size - 1, consumer);
        }
    }

    public static Tile getFacingEdge(Building tile, Building other){
        Tile res = getFacingEdge(tile.block, tile.tileX(), tile.tileY(), other.tile);
        return res == null ? tile.tile : res;
    }

    public static Tile getFacingEdge(Tile tile, Tile other){
        Tile res = getFacingEdge(tile.block, tile.x, tile.y, other);
        return res == null ? tile : res;
    }

    public static Tile getFacingEdge(Block block, int tilex, int tiley, Tile other){
        if(!block.isMultiblock()) return world.tile(tilex, tiley);
        int size = block.size;

        float angle = Angles.angle(Hex.worldX(tilex, tiley), Hex.worldY(tiley), other.worldx(), other.worldy());
        int direction = Math.round(angle / 60f);

        Point2 p = Hex.nearby(tilex, tiley, direction, size - 1);
        return world.tile(p.x, p.y);
    }

    public static Vec2[] getPixelPolygon(float radius){
        if(radius < 1 || radius > maxRadius)
            throw new RuntimeException("Polygon size must be between 1 and " + maxRadius);
        return polygons[(int)(radius * 2) - 1];
    }
}
