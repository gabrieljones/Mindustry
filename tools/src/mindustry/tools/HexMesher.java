package mindustry.tools;

import arc.files.*;
import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;

import java.io.*;

public class HexMesher{
    public static void main(String[] args){
        Fi sourceDir = Fi.get("assets-raw/sprites");
        Fi destDir = Fi.get("assets-raw/sprites_hex"); // Temporary output to verify before replacing

        if(!sourceDir.exists()){
            Log.err("Source directory not found: " + sourceDir.absolutePath());
            return;
        }

        sourceDir.walk(file -> {
            if(!file.extEquals("png")) return;

            // Simple heuristic to detect "block" sprites that should be hexed
            // This assumes 32x32 sprites are single tiles.
            // In a real scenario, we might need a more robust way to identify tile sprites.
            // For now, we process everything that looks square-ish and small enough, or rely on file names.

            try{
                Pixmap pix = new Pixmap(file);
                if(pix.width == pix.height && pix.width >= 32 && pix.width <= 128){ // Assuming 32 is tilesize * some scale
                    process(pix, file, destDir.child(file.path().substring(sourceDir.path().length() + 1)));
                }
                pix.dispose();
            }catch(Exception e){
                Log.err("Failed to process " + file.name(), e);
            }
        });
    }

    static void process(Pixmap pix, Fi source, Fi dest){
        int size = pix.width;
        Pixmap out = new Pixmap(size, size);

        // Fill with clear
        out.fill(0);

        // Hexagon mask logic
        // Pointy topped hex
        float centerX = size / 2f;
        float centerY = size / 2f;
        float radius = size / 2f;
        float sqrt3 = Mathf.sqrt3;

        // We can just iterate pixels and check if they are inside the hexagon
        for(int x = 0; x < size; x++){
            for(int y = 0; y < size; y++){
                if(isInHex(x, y, centerX, centerY, radius)){
                    out.set(x, y, pix.get(x, y));
                }
            }
        }

        dest.parent().mkdirs();
        dest.writePng(out);
        out.dispose();
        Log.info("Processed: " + source.name());
    }

    static boolean isInHex(float x, float y, float cx, float cy, float r){
        // Normalize
        float dx = Math.abs(x - cx);
        float dy = Math.abs(y - cy);
        float rFlat = r * Mathf.sqrt3 / 2f; // Inradius (distance to flat side)

        // For pointy-topped, width is sqrt(3)*R, height is 2*R.
        // But assets are usually square.
        // If the sprite is 32x32, we assume it fits a hex.

        // Pointy topped equation:
        // dx * sqrt(3) + dy <= sqrt(3) * r
        // And dx <= r * sqrt(3) / 2

        // Wait, standard pointy top:
        // Flat sides are left and right. Pointy are top and bottom.
        // Width = sqrt(3) * size
        // Height = 2 * size

        // If we fit in a square of side S.
        // Height = S -> size = S/2.
        // Width = sqrt(3) * S/2 = 0.866 * S.

        // So a square sprite has empty space on sides.

        // Condition for pointy topped hex centered at 0,0 with size 'size' (outer radius):
        // |y| <= size
        // |x| * sqrt(3) + |y| <= sqrt(3) * size
        // (This assumes x is horizontal, y is vertical)

        // Here r is S/2.

        // return dy <= r && (dx * Mathf.sqrt3 + dy <= Mathf.sqrt3 * r);
        // Actually, let's just use a simpler check or mask.

        // Standard "Mindustry" tiles might need rotation? Square tiles usually cover the whole area.
        // Transforming a square tile to a hex tile usually involves masking.

        // Using "Pointy Topped" logic:
        // The hex is bounded by lines.
        // Vertical distance from center <= r
        // Sloped lines: y = -sqrt(3)x + C, y = sqrt(3)x + C

        // Let's rely on vector dot products for 6 sides or just the standard inequalities.
        // dx = abs(x - cx)
        // dy = abs(y - cy)
        // a = r * sqrt(3) * 0.5f (half width)
        // r (half height)

        // if (dx > a) return false;
        // if (dy > r) return false; -- already covered by sprite bounds usually
        // return a * r - r * dx - a * dy >= 0;

        // Let's refine for a "pointy topped" hex fitting in 32x32.
        // Height 32. Outer Radius R = 16.
        // Width = 16 * sqrt(3) ~= 27.7.
        // So there are empty strips on left/right.

        float R = r;
        if(dy > R) return false;
        if(dx * Mathf.sqrt3 + dy > R * Mathf.sqrt3) return false;
        return true;
    }
}
