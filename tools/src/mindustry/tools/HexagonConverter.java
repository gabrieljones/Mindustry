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
        float sqrt3 = (float)Math.sqrt(3);

        out.each((x, y) -> {
            float dx = x - cx;
            float dy = y - cy;

            float ang = (float)Math.toDegrees(Math.atan2(dy, dx));
            if(ang < 0) ang += 360f;

            float u = 0, v = 0;
            boolean valid = false;

            if(ang >= 0 && ang < 120){
                // Bottom Rhombus (0 - 120)
                u = dx / R + dy / (R * sqrt3);
                v = (2 * dy) / (R * sqrt3);
                valid = true;
            }else if(ang >= 120 && ang < 240){
                // Top-Left Rhombus (120 - 240)
                u = -dx / R + dy / (R * sqrt3);
                v = -dx / R - dy / (R * sqrt3);
                valid = true;
            }else{
                // Top-Right Rhombus (240 - 360)
                u = -2 * dy / (R * sqrt3);
                v = dx / R - dy / (R * sqrt3);
                valid = true;
            }

            if(valid && u >= 0 && u <= 1 && v >= 0 && v <= 1){
                if(u < v){
                    float temp = u;
                    u = v;
                    v = temp;
                }

                int sx = (int)(u * (w - 1));
                int sy = (int)(v * (h - 1));

                sx = Math.max(0, Math.min(w - 1, sx));
                sy = Math.max(0, Math.min(h - 1, sy));

                out.set(x, y, input.get(sx, sy));
            }
        });

        return out;
    }
}
