package mindustry.tools;

import arc.files.*;
import arc.graphics.*;
import arc.math.*;
import mindustry.*;
import arc.util.*;

public class HexagonConverter{
    public static void main(String[] args){
        //initialize environment for Pixmap
        Vars.headless = true;
        ArcNativesLoader.load();

        if(args.length < 2){
            System.err.println("Usage: HexagonConverter <input.png/dir> <output.png/dir>");
            System.exit(1);
        }

        Fi input = Fi.get(args[0]);
        Fi output = Fi.get(args[1]);

        if(input.isDirectory()){
            output.mkdirs();

            // Ensure we have the base path string
            String basePath = input.path();

            input.walk(f -> {
                if(f.extEquals("png")){
                    // 1. Manually calculate the relative path via string manipulation
                    // We take the full path and remove the base directory part
                    String relPath = f.path().substring(basePath.length());

                    // 2. Map it to the output directory
                    Fi dest = output.child(relPath);

                    Pixmap pix = new Pixmap(f);
                    try{
                        Pixmap hex = process(pix);

                        // 3. Ensure the sub-directories exist in the destination
                        dest.parent().mkdirs();

                        dest.writePng(hex);
                        hex.dispose();
                        Log.info("Processed @ -> @", f.name(), relPath);
                    }catch(Exception e){
                        Log.err("Failed to process @", f.name(), e);
                    }finally{
                        pix.dispose();
                    }
                }
            });
        }else{
            if(!input.exists()){
                // If input doesn't exist, create a dummy one for testing if requested
                if(input.name().equals("test_square.png")){
                    createTestSquare(input);
                } else {
                    System.err.println("Input file not found: " + input.absolutePath());
                    System.exit(1);
                }
            }

            Pixmap pix = new Pixmap(input);
            try{
                Pixmap hex = process(pix);
                output.writePng(hex);
                hex.dispose();
            }finally{
                pix.dispose();
            }
        }
    }

    static void createTestSquare(Fi file){
        int size = 64;
        Pixmap p = new Pixmap(size, size);
        p.each((x, y) -> {
            // Draw something in the top triangle
            if(y < x && y < size - x){ // Top
                p.set(x, y, Color.red.rgba());
            } else if (x < y && y < size - x){ // Left
                p.set(x, y, Color.green.rgba());
            } else if (x > y && y > size - x){ // Right
                p.set(x, y, Color.blue.rgba());
            } else { // Bottom
                p.set(x, y, Color.yellow.rgba());
            }
            // Draw center
            if(Math.abs(x - size/2) < 2 && Math.abs(y - size/2) < 2) p.set(x, y, Color.white.rgba());
        });
        file.writePng(p);
        p.dispose();
    }

    public static Pixmap process(Pixmap input){
        int w = input.width;
        int h = input.height;
        // Assume square input
        if(w != h) throw new IllegalArgumentException("Input must be square");

        // Output Hexagon dimensions
        // Let side length R = w.
        // Flat topped hexagon width = 2 * R = 2 * w.
        // Height = sqrt(3) * R = sqrt(3) * w.

        int R = w;
        int outW = 2 * R;
        int outH = (int)Math.ceil(Math.sqrt(3) * R);

        Pixmap out = new Pixmap(outW, outH);

        // Center of hexagon in output image
        float cx = outW / 2f;
        float cy = outH / 2f;

        // Square center
        float sqCx = w / 2f;
        float sqCy = h / 2f;

        out.each((x, y) -> {
            float dx = x - cx;
            float dy = y - cy;

            // Check if inside hexagon (flat topped)
            // Sides at x = +/- R
            // Diagonals: |dx| * sqrt(3) + |dy| <= R * sqrt(3) ???
            // Let's re-derive flat topped hexagon bounds.
            // Width 2R. Height sqrt(3)R.
            // Center (0,0).
            // Top/Bottom edges: y = +/- R*sqrt(3)/2.
            // Diagonal edges:
            // Vertex (R, 0) to (R/2, R*sqrt(3)/2).
            // Slope = (R*sqrt(3)/2 - 0) / (R/2 - R) = (R*sqrt(3)/2) / (-R/2) = -sqrt(3).
            // Line: Y - 0 = -sqrt(3) * (X - R).
            // Y = -sqrt(3)X + R*sqrt(3).
            // sqrt(3)X + Y = R*sqrt(3).
            // Generally: sqrt(3)*|dx| + |dy| <= R*sqrt(3).
            // WAIT. My vertex coordinates earlier were: (R, 0).
            // Correct.
            // For flat topped hex:
            // |dx| <= R (No, vertices are at (+/- R, 0)).
            // |dy| <= R*sqrt(3)/2.
            // AND
            // |dx| * 1/2 + |dy| * sqrt(3)/2 <= R*sqrt(3)/2 ?
            // Let's check (R, 0): R/2 + 0 <= R*sqrt(3)/2 -> 1 <= sqrt(3). True.
            // Let's check (R/2, R*sqrt(3)/2): R/4 + 3R/4 = R <= R*sqrt(3)/2 ?
            // 1 <= 0.866 ? FALSE.
            // So my inequality is wrong.

            // Re-check vertices of flat-topped hexagon with side R.
            // Center (0,0).
            // Vertices: (+/- R, 0), (+/- R/2, +/- R*sqrt(3)/2).
            // Bounds:
            // |dy| <= R*sqrt(3)/2.
            // |dx| <= R (implied by diagonals if they are correct).
            // Diagonals connect (R, 0) and (R/2, R*sqrt(3)/2).
            // Line equation: Y = -sqrt(3)(X - R). => Y + sqrt(3)X = R*sqrt(3).
            // So sqrt(3)*|dx| + |dy| <= R*sqrt(3) ?
            // Check (R/2, R*sqrt(3)/2): sqrt(3)*R/2 + R*sqrt(3)/2 = R*sqrt(3). Correct.
            // Check (R, 0): sqrt(3)*R + 0 = R*sqrt(3). Correct.
            // So inequality is: sqrt(3)*|dx| + |dy| <= R*sqrt(3).
            // Dividing by sqrt(3): |dx| + |dy|/sqrt(3) <= R.

            if(Math.abs(dy) > R * Math.sqrt(3) / 2) return;
            if(Math.abs(dx) + Math.abs(dy) / Math.sqrt(3) > R) return;

            // Convert to polar
            float angRad = (float)Math.atan2(dy, dx);
            float dist = (float)Math.sqrt(dx*dx + dy*dy);

            float angDeg = (float)Math.toDegrees(angRad);

            // Normalize to -30..30 relative to North (-90).
            // North is -90.
            float relativeAngle = angDeg + 90;
            while(relativeAngle > 30) relativeAngle -= 60;
            while(relativeAngle < -30) relativeAngle += 60;

            // Map back to North sector coordinates (centered at -90 deg)
            float canAngRad = (float)Math.toRadians(-90 + relativeAngle);
            float rx = dist * (float)Math.cos(canAngRad);
            float ry = dist * (float)Math.sin(canAngRad);

            // Map (rx, ry) to Square Top Triangle
            // sqX = w/2 + rx
            // sqY = w/2 + ry / sqrt(3)

            float srcX = sqCx + rx;
            float srcY = sqCy + ry / (float)Math.sqrt(3);

            int sx = Math.max(0, Math.min(w-1, (int)srcX));
            int sy = Math.max(0, Math.min(h-1, (int)srcY));

            out.set(x, y, input.get(sx, sy));
        });

        return out;
    }
}
