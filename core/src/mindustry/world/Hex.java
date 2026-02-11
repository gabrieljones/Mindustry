package mindustry.world;

import arc.math.*;
import arc.math.geom.*;
import mindustry.Vars;

public class Hex{
    // Odd-r offset coordinates (pointy topped)
    // Neighbors for even rows
    // 0: Right, 1: Bottom Right, 2: Bottom Left, 3: Left, 4: Top Left, 5: Top Right (Assuming Y increases downwards)
    public static final Point2[] neighborsEven = {
        new Point2(1, 0), new Point2(0, 1), new Point2(-1, 1), new Point2(-1, 0), new Point2(-1, -1), new Point2(0, -1)
    };
    // Neighbors for odd rows
    public static final Point2[] neighborsOdd = {
        new Point2(1, 0), new Point2(1, 1), new Point2(0, 1), new Point2(-1, 0), new Point2(0, -1), new Point2(1, -1)
    };

    public static Point2 getOffset(int y, int direction){
        Point2[] neighbors = ((y & 1) == 0) ? neighborsEven : neighborsOdd;
        // ensure direction is 0-5
        int dir = (direction % 6 + 6) % 6;
        return neighbors[dir];
    }

    public static Point2 nearby(int x, int y, int direction){
        Point2 offset = getOffset(y, direction);
        return new Point2(x + offset.x, y + offset.y);
    }

    public static Point2 nearby(int x, int y, int direction, int length){
        for(int i = 0; i < length; i++){
            Point2 p = nearby(x, y, direction);
            x = p.x;
            y = p.y;
        }
        return new Point2(x, y);
    }

    public static float worldX(int x, int y){
        return (x + (y & 1) * 0.5f) * Vars.tilesize;
    }

    public static float worldY(int y){
        return y * (Vars.tilesize * Mathf.sqrt3 / 2f);
    }

    public static Point2 worldToTile(float wx, float wy){
        float hexH = Vars.tilesize * Mathf.sqrt3 / 2f;
        int y = Math.round(wy / hexH);

        float xOffset = (y & 1) * 0.5f * Vars.tilesize;
        int x = Math.round((wx - xOffset) / Vars.tilesize);

        // Refine by checking neighbors for closest center
        Point2 best = new Point2(x, y);
        float bestDst = Mathf.dst2(wx, wy, worldX(x, y), worldY(y));

        for(int i = 0; i < 6; i++){
            Point2 p = nearby(x, y, i);
            float d = Mathf.dst2(wx, wy, worldX(p.x, p.y), worldY(p.y));
            if(d < bestDst){
                bestDst = d;
                best = p;
            }
        }

        return best;
    }
}
