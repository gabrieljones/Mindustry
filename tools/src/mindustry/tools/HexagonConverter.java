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

        // Output Hexagon dimensions (Pointy Topped)
        // Let side length R = w.
        // Width = sqrt(3) * R.
        // Height = 2 * R.

        int R = w;
        int outW = (int)Math.ceil(Math.sqrt(3) * R);
        int outH = 2 * R;

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
            boolean lowerLeft = false;

            if(ang >= 30 && ang < 150){
                // Bottom (actually, visual Bottom is 30-150? No. Image coords Y down.)
                // 0 is Right. 90 is Down.
                // Pointy Topped Hexagon:
                // Vertices at 30, 90, 150, 210, 270, 330.
                // Edges at 0, 60, 120, 180, 240, 300. (Flat sides are vertical? No.)
                // Pointy Topped: Top vertex is at -90 (Up) or 90 (Down)?
                // "Point Up". Usually means Vertex at Top.
                // In Y-down: Vertex at -90 (Up).
                // Vertices at -90(270), -30(330), 30, 90, 150, 210.
                // Rhombi axes: 30, 150, 270.

                // Sectors between axes:
                // 1. 270 to 30 (Top-Right visual? No. -90 to 30. North-East).
                // 2. 30 to 150 (South / Bottom).
                // 3. 150 to 270 (North-West / Top-Left).

                // Wait. 3 rhombi meet at center.
                // Top Rhombus: Bounded by 210(-150) and 330(-30)? (North sector).
                // Bottom-Right Rhombus: Bounded by 330(-30) and 90? (South-East).
                // Bottom-Left Rhombus: Bounded by 90 and 210? (South-West).

                // Let's re-verify Pointy Topped with Y-down.
                // Vertices: (0, -R), (+-W/2, -R/2), (+-W/2, R/2), (0, R).
                // Angles from center (0,0):
                // (0, -R) -> -90 deg (270).
                // (W/2, -R/2) -> -30 deg (330).
                // (W/2, R/2) -> 30 deg.
                // (0, R) -> 90 deg.
                // (-W/2, R/2) -> 150 deg.
                // (-W/2, -R/2) -> 210 deg.

                // Rhombi boundaries are the lines connecting Center to alternating vertices?
                // No, Rhombi boundaries are lines connecting Center to Vertices (0, -R), (-W/2, R/2), (W/2, R/2)?
                // Standard "Cube" view: Y axis vertical.
                // 3 faces: Top, Left, Right? No.
                // Usually Top, Bottom-Left, Bottom-Right.
                // Or Left, Right, Top?

                // Let's assume standard "Y" split.
                // The "Y" is formed by edges from Center to (0, -R), (W/2, R/2), (-W/2, R/2)? No.
                // That would be an inverted Y.
                // "Point Up" Hexagon usually looks like a cube with a Top face.
                // Edges from Center go to (0, -R) [Up], and (W/2, R/2) [Down-Right], (-W/2, R/2) [Down-Left].
                // No, that divides into 3 regions?
                // Region 1 (Top-Right): Between -90 and 30?
                // Region 2 (Bottom): Between 30 and 150?
                // Region 3 (Top-Left): Between 150 and 270(-90)?

                // If the edges from center go to vertices at 330(-30), 90, 210?
                // Then we have:
                // Top Rhombus: Between 210 and 330. (Top sector).
                // Bottom-Right: Between 330 and 90.
                // Bottom-Left: Between 90 and 210.

                // My Verification Plan assumed:
                // Top Rhombus Center at (cx, cy - R/2). This is angle -90 (270).
                // So Top Rhombus is the sector around -90.
                // Boundaries: 210 to 330 (-150 to -30). Correct.

                // Bottom-Right Rhombus Center at (cx + dx, cy + dy). Angle 30?
                // Boundaries: 330 to 90 (-30 to 90). Correct.

                // Bottom-Left Rhombus Center at (cx - dx, cy + dy). Angle 150?
                // Boundaries: 90 to 210. Correct.

                // New logic:
                // Top Rhombus: 210 - 330 (-150 to -30).
                // Bottom-Right: 330 - 90 (-30 to 90).
                // Bottom-Left: 90 - 210.
            }

            // Adjust angle to be 0-360 positive
            // 210-330 is 210-330.
            // 330-90 is 330-360 and 0-90.
            // 90-210 is 90-210.

            if(ang >= 90 && ang < 210){
                // Bottom-Left Rhombus
                // Map to Lower-Left Quadrant of Source.
                // Center -> (0.5, 0.5).
                // Outer Vertex (at 150 deg) -> (0, 1) [Bottom-Left of Source].

                // Basis vectors for Rhombus:
                // V1 (at 90 deg) -> ?
                // V2 (at 210 deg) -> ?
                // Mapping P = C + u*A + v*B ?
                // Or simplified linear map?
                // Rotated/Scaled map.

                // Let's use direct projection.
                // Rotate point by -150 deg (align 150 to 0).
                // Scale.

                // Or better:
                // Vector C -> P.
                // Rotate/Skew to map C->(0.5, 0.5) and V_outer->(0, 1).

                // Standard Rhombus u,v (0..1) from Center to Edges?
                // Let's calculate coords relative to axes at 90 and 210.
                // Axis 1 (v axis): 90 deg. (0, 1).
                // Axis 2 (u axis): 210 deg. (-sqrt(3)/2, -0.5).
                // P = u * Ax1 + v * Ax2?
                // Center is origin.
                // If P is on 150 deg (Bisector), u=v.
                // 150 deg vector: (-sqrt(3)/2, 0.5).
                // Ax1 + Ax2 = (0, 1) + (-sqrt(3)/2, -0.5) = (-sqrt(3)/2, 0.5). Matches bisector.
                // So u, v range 0..1 covers the rhombus.
                // V_outer (at 150) is u=1, v=1.
                // Center is u=0, v=0.

                // Source Mapping:
                // C (0,0) -> (0.5, 0.5).
                // V_outer (1,1) -> (0, 1).
                // This means u,v (1,1) maps to displacement (-0.5, 0.5).
                // So Source = (0.5, 0.5) + u * VecA + v * VecB?
                // If u=1, v=1 -> (-0.5, 0.5).
                // VecA + VecB = (-0.5, 0.5).
                // We need to define VecA and VecB.
                // Ideally symmetric.
                // Maybe VecA = (-0.5, 0)? VecB = (0, 0.5)?
                // Then (0.5, 0.5) + u(-0.5, 0) + v(0, 0.5) = (0.5 - 0.5u, 0.5 + 0.5v).
                // Check corners:
                // u=1, v=0 (Axis 2, 210 deg) -> (0, 0.5). Mid-left edge.
                // u=0, v=1 (Axis 1, 90 deg) -> (0.5, 1). Bottom-mid edge.
                // u=1, v=1 (Outer, 150 deg) -> (0, 1). Bottom-Left corner. Correct.
                // u=0, v=0 (Center) -> (0.5, 0.5). Correct.

                // So for Bottom-Left:
                // u = component along 210 deg.
                // v = component along 90 deg.
                // sx = 0.5 - 0.5 * u
                // sy = 0.5 + 0.5 * v

                // Calculate u, v:
                // P = u * V210 + v * V90.
                // V90 = (0, R).
                // V210 = (-R*sqrt(3)/2, -R/2).
                // dx = u * (-R*sqrt(3)/2) + v * 0
                // dy = u * (-R/2) + v * R
                // Solve for u: u = dx / (-R*sqrt(3)/2) = -2*dx / (R*sqrt(3)).
                // Solve for v: v*R = dy - u*(-R/2) = dy + u*R/2.
                // v = dy/R + u/2.

                u = -2 * dx / (R * sqrt3);
                v = dy / R + u / 2;

                // Map to source (Bottom-Left logic)
                // sx = 0.5 * (1 - u).
                // sy = 0.5 * (1 + v).
                // Wait. u, v are 0..1.
                // Check bounds: valid if u>=0, v>=0, u<=1, v<=1?
                // Actually Rhombus is u in [0,1], v in [0,1].

                float su = 0.5f * (1f - u);
                float sv = 0.5f * (1f + v);

                // Mirroring for "Lower Left Triangle" preference?
                // Lower Left Triangle is y > x.
                // Our mapping puts us in [0, 0.5] x [0.5, 1].
                // Here y > 0.5, x < 0.5. So y > x is guaranteed?
                // 0.5 > 0.5? No.
                // But generally y is large, x is small.
                // Max x is 0.5. Min y is 0.5.
                // So y >= x always.
                // No mirroring needed.

                u = su;
                v = sv;
                valid = true;

            } else if(ang >= 210 && ang < 330){
                // Top Rhombus
                // Axes: 210 and 330 (-30).
                // Bisector: 270 (-90). Top vertical.
                // V_outer at 270.

                // Axes vectors:
                // V210 = (-R*sqrt(3)/2, -R/2).
                // V330 = (R*sqrt(3)/2, -R/2).

                // P = u * V210 + v * V330.
                // dx = u * (-sqrt(3)/2)R + v * (sqrt(3)/2)R = R*sqrt(3)/2 * (v - u).
                // dy = u * (-R/2) + v * (-R/2) = -R/2 * (u + v).

                // u + v = -2*dy / R.
                // v - u = 2*dx / (R*sqrt(3)).
                // Adding: 2v = -2*dy/R + 2*dx/(R*sqrt(3)).
                // v = -dy/R + dx/(R*sqrt(3)).
                // Subtracting: 2u = -2*dy/R - 2*dx/(R*sqrt(3)).
                // u = -dy/R - dx/(R*sqrt(3)).

                u = -dy / R - dx / (R * sqrt3);
                v = -dy / R + dx / (R * sqrt3);

                // Map to Upper Right Triangle (Top-Right Quadrant).
                // C -> (0.5, 0.5).
                // V_outer (270) -> (1, 0)? (Top-Right of source).
                // Check V_outer coords in u,v:
                // V_outer = V210 + V330?
                // (-sqrt3/2, -0.5) + (sqrt3/2, -0.5) = (0, -1). Yes, (0, -R). Correct.
                // So at V_outer, u=1, v=1.

                // Map (u=1, v=1) -> (1, 0).
                // (u=0, v=0) -> (0.5, 0.5).
                // Delta = (0.5, -0.5).
                // Let's split delta between u and v symmetric?
                // sx = 0.5 + 0.5 * u? (At u=1 -> 1.0).
                // sy = 0.5 - 0.5 * v? (At v=1 -> 0.0).
                // Let's check cross terms.
                // If u=1, v=0 -> (1.0, 0.5). Mid-Right edge.
                // If u=0, v=1 -> (0.5, 0.0). Top-Mid edge.
                // If u=1, v=1 -> (1.0, 0.0). Top-Right Corner.
                // Seems correct.

                // Check Upper Right Triangle ($x > y$).
                // Quadrant x:[0.5, 1], y:[0, 0.5].
                // x is large, y is small.
                // x > y guaranteed?
                // Min x 0.5. Max y 0.5.
                // So x >= y always.
                // No mirroring needed.

                float su = 0.5f * (1f + u); // u goes to x
                float sv = 0.5f * (1f - v); // v goes to y (inverted)

                // Wait. Is u associated with Left axis (210) and v with Right axis (330)?
                // V210 is Left. V330 is Right.
                // u=1, v=0 -> V210 -> (1.0, 0.5). (Right edge of source?)
                // Wait. Source (1,0) is Top-Right.
                // Source (0.5, 0.5) Center.
                // Source (1, 0.5) Right-Center.
                // Source (0.5, 0) Top-Center.
                // If u maps to +x, u corresponds to Right?
                // But u is V210 (Left).
                // If u=1 (Left Hex), sx=1 (Right Source). Inverted?
                // Maybe swap u,v?
                // If we want Left Hex side to map to Top Source side?
                // And Right Hex side to map to Right Source side?
                // u=1 (Left) -> sy=0 (Top).
                // v=1 (Right) -> sx=1 (Right).

                // New map:
                // sx = 0.5 + 0.5 * v. (v contributes to Right).
                // sy = 0.5 - 0.5 * u. (u contributes to Top/Up).
                // Check V_outer (u=1, v=1) -> (1, 0). Correct.
                // Check u=1, v=0 (Left Axis) -> (0.5, 0). Top-Center. Correct.
                // Check u=0, v=1 (Right Axis) -> (1, 0.5). Right-Center. Correct.

                float tempU = u;
                u = 0.5f + 0.5f * v;
                v = 0.5f - 0.5f * tempU;

                valid = true;

            } else {
                // Bottom-Right Rhombus (330 to 90).
                // Axes: 330 and 90.
                // V_outer at 30? No, 30 is between 330 and 90.
                // 30 deg is (sqrt(3)/2, 0.5).
                // V330 = (sqrt(3)/2, -0.5).
                // V90 = (0, 1).
                // V330 + V90 = (sqrt(3)/2, 0.5). Matches V30. Correct.

                // P = u * V330 + v * V90.
                // dx = u * (R*sqrt(3)/2).
                // dy = u * (-R/2) + v * R.

                // u = 2*dx / (R*sqrt(3)).
                // v = (dy + u*R/2) / R = dy/R + u/2.

                u = 2 * dx / (R * sqrt3);
                v = dy / R + u / 2;

                // Map to Upper Right Triangle?
                // Or maybe just map to Top-Right Quadrant again?
                // Or "Lower Right"?
                // User said "take the upper right triangle" for the source.
                // But "Use lower left triangle for bottom left rhombus".
                // Doesn't explicitly say for Bottom-Right.
                // Assume Upper Right Triangle (consistent with "take the upper right triangle" general instruction).

                // Map to Top-Right Quadrant (towards (1,0))?
                // C -> (0.5, 0.5).
                // V_outer (30 deg) -> (1, 0)?

                // V_outer is u=1, v=1.
                // u=1 (Axis 330) -> ?
                // v=1 (Axis 90) -> ?

                // If we want sx=1, sy=0 (Top Right).
                // Maybe u contributes to Right (x)? v contributes to Top (y)?
                // u=1 (330, Up-Right) -> sx=1?
                // v=1 (90, Down) -> sy=0? (Inverted?)

                // Try:
                // sx = 0.5 + 0.5 * u.
                // sy = 0.5 - 0.5 * v.
                // u=1, v=1 -> (1, 0). Correct.
                // u=1, v=0 (330) -> (1, 0.5). Right-Center.
                // u=0, v=1 (90) -> (0.5, 0). Top-Center?
                // Wait. V90 is Down.
                // Mapping Down to Top-Center?
                // V330 is Up-Right. Mapping to Right-Center.
                // V_outer (30) is Down-Right (visual). (0.866, 0.5).
                // Mapping Down-Right to Top-Right (1,0).
                // This implies a flip/rotation.
                // Seems acceptable as long as it's the Upper Right Triangle.
                // In Top-Right Quadrant: x>=y is guaranteed.

                float tempU = u;
                u = 0.5f + 0.5f * tempU;
                v = 0.5f - 0.5f * v;

                valid = true;
            }

            if(valid && u >= 0 && u <= 1 && v >= 0 && v <= 1){
                // u, v are now source coordinates (normalized 0..1).
                // Map to integer pixels.

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
