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
        Fi destDir = Fi.get("assets-raw/sprites_hex"); // Output directory

        if(!sourceDir.exists()){
            Log.err("Source directory not found: " + sourceDir.absolutePath());
            return;
        }

        sourceDir.walk(file -> {
            if(!file.extEquals("png")) return;

            try{
                Pixmap pix = new Pixmap(file);
                // Heuristic: If it's a square block/floor sprite
                if(pix.width == pix.height && pix.width >= 32 && pix.width <= 128){
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

        // 1. Skew the square to make a rhombus (top-left aligned, skewed right)
        // Angle = 30 degrees?
        // For a hexagon, we need 3 rhombi.
        // A rhombus for a hex has angles 60 and 120.
        // Input square has 90.
        // Transformation:
        // x' = x + y * 0.5
        // y' = y * 0.866 (sin 60)

        // Let's create a rhombus pixmap.
        int rhW = size;
        int rhH = (int)(size * Mathf.sinDeg(60));
        Pixmap rhombus = new Pixmap(size + (int)(size * 0.5f), rhH);
        rhombus.fill(0); // Clear

        for(int y = 0; y < size; y++){
            for(int x = 0; x < size; x++){
                int px = x + (int)(y * 0.5f); // Skew x by y
                int py = (int)(y * Mathf.sinDeg(60)); // Scale y
                // Nearest neighbor for now
                if(px < rhombus.width && py < rhombus.height){
                    rhombus.setRaw(px, py, pix.getRaw(x, y));
                }
            }
        }

        // 2. Assemble 3 rhombi into a hexagon
        // Center is the meeting point.
        // Rhombus 1: Top (Rotate 0? No, standard cube orientation)
        // Top face, Left face, Right face.

        // Let's assume the texture is for the "Top" face.
        // Then we copy it for Left and Right faces, rotating appropriately.
        // Actually for a 2D hex grid floor, we usually just want the "top" face if it's flat.
        // But "convert square to hex" usually implies mapping the square texture onto the hex shape.
        // If we map it to 3 rhombi, it looks like a 3D cube.
        // If we want a flat hex, we just mask the center?
        // The user said: "skew the top to the right to make a rhombus - copy and rotate around the lower right corner twice".
        // This creates a hexagon from 3 rhombi.

        // Let's implement that specific instruction.

        // "skew the top to the right to make a rhombus" -> We did that (mostly).
        // "copy and rotate around the lower right corner twice"

        // Rhombus 1 (Top-Left origin, but we need to pivot around lower right)
        // Let's refine the rhombus creation to match "skew top to right".
        // Square (0,0) to (S,S).
        // Top edge (0,0)-(S,0) -> (0,0)-(S,0)
        // Bottom edge (0,S)-(S,S) -> skewed?
        // Usually "skew X" means x' = x + ky.
        // k = tan(30) or cot(60)?
        // To get a 60-degree rhombus from a 90-degree square.
        // We want (0,0), (S,0) to stay.
        // (0,S) should move to ...

        // Let's stick to the "3 rhombi make a hex" approach.
        // Rhombus size S. Hex bounding box is roughly 2S x 1.73S.

        int hexW = (int)(size * 2 * Mathf.sinDeg(60)); // Approx 1.73 * S? No.
        // If side is S. Width is 2*S*sin(60) = S*sqrt(3).
        // Height is 2*S? No, from corner to corner.
        // Side length of rhombus = S.
        // Hexagon width (flat to flat) = 2 * S * sin(60) = S * 1.732.
        // Hexagon height (point to point) = 2 * S.

        // We need a canvas of size approx 2*S.
        Pixmap out = new Pixmap(size * 2, size * 2);
        out.fill(0);

        // Center of output
        int cx = size;
        int cy = size;

        // Helper to draw skewed, rotated rhombus
        // Rotation 0: Top-Left rhombus (standard?)
        // The user said "rotate around the lower right corner".
        // Rhombus 1: P1=(0,0), P2=(S,0), P3=(S+dx, S), P4=(dx, S).
        // Lower Right of Rhombus 1 is (S+dx, S)? Or (S, S)?

        // Let's try to simulate the geometry.
        // Center C.
        // Rhombus 1: C is bottom-right corner.
        // Rhombus 2: Rotated 120 deg around C.
        // Rhombus 3: Rotated 240 deg around C.

        for(int i = 0; i < 3; i++){
            // For each pixel in source square
            for(int y = 0; y < size; y++){
                for(int x = 0; x < size; x++){
                    int color = pix.getRaw(x, y);
                    if((color & 0xFF) == 0) continue; // Skip alpha 0

                    // Map square (x,y) to Rhombus (u,v) relative to bottom-right corner (which is 0,0 in local frame)
                    // Source bottom-right is (size, size).
                    // dx = x - size
                    // dy = y - size

                    float dx = x - size;
                    float dy = y - size;

                    // Skew transform (making it a rhombus with 60/120 angles)
                    // Transform: (x,y) -> (x - y*0.5, y*0.866) ?
                    // Or (x * 1 + y * cos(60), y * sin(60))
                    // Let's use:
                    // u = dx + dy * 0.5
                    // v = dy * 0.866

                    float u = dx - dy * 0.5f; // Skewing top to right relative to bottom?
                    float v = dy * Mathf.sqrt3 / 2f;

                    // Now rotate by i * 120 degrees
                    float angle = i * 120 + 30; // +30 offset? Or just i*120?
                    // Square corner at C is 90 deg. Rhombus corner at C is 120 deg?
                    // 3 * 120 = 360.
                    // So we map the 90 deg corner of square to 120 deg corner of rhombus?
                    // That implies non-linear or stretching?
                    // Or we just place the rhombus such that its 120-deg corner is at C.

                    // Rotate (u, v)
                    float cos = Mathf.cosDeg(angle);
                    float sin = Mathf.sinDeg(angle);

                    float rx = u * cos - v * sin;
                    float ry = u * sin + v * cos;

                    // Translate to center
                    int finalX = (int)(cx + rx);
                    int finalY = (int)(cy + ry);

                    if(finalX >= 0 && finalX < out.width && finalY >= 0 && finalY < out.height){
                        out.setRaw(finalX, finalY, color);
                    }
                }
            }
        }

        dest.parent().mkdirs();
        out.writePng(dest);
        out.dispose();
        Log.info("Meshed: " + source.name());
    }
}
