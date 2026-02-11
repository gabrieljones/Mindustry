package mindustry.tools;

import arc.files.*;
import arc.graphics.*;
import arc.struct.*;
import arc.util.*;

import java.io.*;

public class RasterToVector{
    public static void main(String[] args){
        Fi sourceDir = Fi.get("core/assets-raw/sprites");
        Fi destDir = Fi.get("core/assets-raw/sprites_vector");

        if(!sourceDir.exists()){
            // Try fallback path if running from root
            sourceDir = Fi.get("core/assets-raw/sprites");
            if(!sourceDir.exists()){
                Log.err("Source directory not found: " + sourceDir.absolutePath());
                return;
            }
        }

        Fi finalSourceDir = sourceDir;
        sourceDir.walk(file -> {
            if(!file.extEquals("png")) return;

            try{
                process(file, destDir.child(file.path().substring(finalSourceDir.path().length() + 1)).sibling(file.nameWithoutExtension() + ".svg"));
            }catch(Exception e){
                Log.err("Failed to process " + file.name(), e);
            }
        });
    }

    static void process(Fi source, Fi dest){
        Pixmap pix = new Pixmap(source);
        StringBuilder svg = new StringBuilder();

        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ").append(pix.width).append(" ").append(pix.height).append("\">\n");

        // Naive row-based optimization
        for(int y = 0; y < pix.height; y++){
            for(int x = 0; x < pix.width; x++){
                int raw = pix.getRaw(x, y);
                if((raw & 0x000000ff) == 0) continue; // Skip fully transparent pixels

                int w = 1;
                // Look ahead
                while(x + w < pix.width && pix.getRaw(x + w, y) == raw){
                    w++;
                }

                Color c = new Color(raw);
                String hex = "#" + c.toString().substring(0, 6);
                float alpha = c.a;

                svg.append("<rect x=\"").append(x).append("\" y=\"").append(y)
                   .append("\" width=\"").append(w).append("\" height=\"1\" fill=\"").append(hex).append("\"");

                if(alpha < 1f){
                    svg.append(" opacity=\"").append(alpha).append("\"");
                }
                svg.append("/>\n");

                x += w - 1;
            }
        }

        svg.append("</svg>");

        dest.parent().mkdirs();
        dest.writeString(svg.toString());
        pix.dispose();
        Log.info("Vectorized: " + source.name());
    }
}
