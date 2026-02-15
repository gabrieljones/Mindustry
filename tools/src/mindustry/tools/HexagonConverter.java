package mindustry.tools;

import arc.files.*;
import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
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
                    if(f.path().contains("conveyor") || f.path().contains("conduit") || f.path().contains("duct") || f.path().contains("autotile")){
                        return;
                    }

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

    static Vec2 getUV(float dx, float dy, float R){
        float sqrt3 = (float)Math.sqrt(3);
        float ang = (float)Math.toDegrees(Math.atan2(dy, dx));
        if(ang < 0) ang += 360f;

        float u = 0, v = 0;
        boolean valid = false;

        if(ang >= 90 && ang < 210){
            // Bottom-Left
            u = -2 * dx / (R * sqrt3);
            v = dy / R + u / 2;

            float su = 0.5f * (1f - u);
            float sv = 0.5f * (1f + v);
            u = su; v = sv;
            valid = true;
        }else if(ang >= 210 && ang < 330){
            // Top
            u = -dy / R - dx / (R * sqrt3);
            v = -dy / R + dx / (R * sqrt3);

            float tempU = u;
            u = 0.5f + 0.5f * v;
            v = 0.5f - 0.5f * tempU;
            valid = true;
        }else{
            // Bottom-Right
            u = 2 * dx / (R * sqrt3);
            v = dy / R + u / 2;

            float tempU = u;
            u = 0.5f + 0.5f * tempU;
            v = 0.5f - 0.5f * v;
            valid = true;
        }

        if(valid && u >= 0 && u <= 1 && v >= 0 && v <= 1){
            return new Vec2(u, v);
        }
        return null;
    }

    public static Pixmap process(Pixmap input){
        int w = input.width;
        int h = input.height;
        // Assume square input
        if(w != h) throw new IllegalArgumentException("Input must be square");

        if(w % 32 == 0 && w / 32 > 1){
            return processCluster(input, w / 32);
        }

        // Output Hexagon dimensions (Pointy Topped)
        // Let side length R = w.
        // Width = sqrt(3) * R.
        // Height = 2 * R.

        int R = (int)(w / 1.85f);
        int outW = (int)Math.ceil(Math.sqrt(3) * R);
        int outH = 2 * R;

        Pixmap out = new Pixmap(outW, outH);

        // Center of hexagon in output image
        float cx = outW / 2f;
        float cy = outH / 2f;

        out.each((x, y) -> {
            float dx = x - cx;
            float dy = y - cy;

            Vec2 uv = getUV(dx, dy, R);
            if(uv != null){
                int sx = (int)(uv.x * (w - 1));
                int sy = (int)(uv.y * (h - 1));

                sx = Math.max(0, Math.min(w - 1, sx));
                sy = Math.max(0, Math.min(h - 1, sy));

                out.set(x, y, input.get(sx, sy));
            }
        });

        return out;
    }

    public static Pixmap processCluster(Pixmap input, int size){
        int radius = size - 1;
        float hexR = 17f;
        float hexW = (float)(Math.sqrt(3) * hexR);

        int outW = (int)Math.ceil((radius * 2 + 1) * hexW);
        int outH = (int)((radius * 3 + 2) * hexR) + 1;

        Pixmap out = new Pixmap(outW, outH);
        float cx = outW / 2f, cy = outH / 2f;

        out.each((x, y) -> {
            float dx = x - cx;
            float dy = y - cy;

            //convert to axial coords
            float q = ((float)Math.sqrt(3)/3f * dx - 1f/3f * dy) / hexR;
            float r = (2f/3f * dy) / hexR;

            float x3 = q, z3 = r, y3 = -x3 - z3;
            int rx = Math.round(x3), rz = Math.round(z3), ry = Math.round(y3);

            float x_diff = Math.abs(rx - x3);
            float y_diff = Math.abs(ry - y3);
            float z_diff = Math.abs(rz - z3);

            if(x_diff > y_diff && x_diff > z_diff){
                rx = -ry - rz;
            }else if(y_diff > z_diff){
                ry = -rx - rz;
            }else{
                rz = -rx - ry;
            }

            int dist = Math.max(Math.abs(rx), Math.max(Math.abs(ry), Math.abs(rz)));

            if(dist <= radius){
                // Compute the hex center in output pixels
                // dx = (q + 0.5f * r) * hexW
                // dy = r * 1.5f * hexR
                // relative to cx, cy

                float hexCx = cx + (rx + rz * 0.5f) * hexW;
                float hexCy = cy + rz * 1.5f * hexR;

                // Offset from this hex center
                float pdx = x - hexCx;
                float pdy = y - hexCy;

                Vec2 uv = getUV(pdx, pdy, hexR);

                if(uv != null){
                     // Map to input image sub-region
                     // Sub-region center corresponds to hex center
                     // Tile size in input image:
                     float tileW = hexW / outW * input.width;
                     float tileH = (2 * hexR) / outH * input.height;

                     float tileCenterX = hexCx / outW * input.width;
                     float tileCenterY = hexCy / outH * input.height;

                     // uv is 0..1 (0.5 is center)
                     float sxFloat = tileCenterX + (uv.x - 0.5f) * tileW;
                     float syFloat = tileCenterY + (uv.y - 0.5f) * tileH;

                     int sx = (int)sxFloat;
                     int sy = (int)syFloat;

                     out.set(x, y, input.get(Math.max(0, Math.min(sx, input.width - 1)), Math.max(0, Math.min(sy, input.height - 1))));
                }
            }
        });

        return out;
    }
}
