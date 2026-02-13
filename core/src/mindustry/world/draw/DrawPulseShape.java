package mindustry.world.draw;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;

import static mindustry.Vars.*;

public class DrawPulseShape extends DrawBlock{
    public Color color = Pal.accent.cpy();
    public float stroke = 2f, timeScl = 100f, minStroke = 0.2f;
    public float radiusScl = 1f;
    public float layer = -1f;
    public boolean square = true;

    public DrawPulseShape(boolean square){
        this.square = square;
    }

    public DrawPulseShape(){
    }

    @Override
    public void draw(Building build){
        float pz = Draw.z();
        if(layer > 0) Draw.z(layer);

        float f = 1f - (Time.time / timeScl) % 1f;
        float rad = build.block.size * tilesize / 2f * radiusScl;

        Draw.color(color);
        Lines.stroke((stroke * f + minStroke) * build.warmup());

        if(square){
            Lines.square(build.x, build.y, Math.min(1f + (1f - f) * rad, rad));
        }else{
            float r = Math.max(0f, Mathf.clamp(2f - f * 2f) * rad - f - 0.2f), w = Mathf.clamp(0.5f - f) * rad * 2f;
            Lines.beginLine();
            for(int i = 0; i < 6; i++){
                float angle = i * 60f;
                Tmp.v1.trns(angle, r).add(build.x, build.y);
                Tmp.v2.trns(angle - 90, w);
                Lines.linePoint(Tmp.v1.x + Tmp.v2.x, Tmp.v1.y + Tmp.v2.y);

                if(f < 0.5f){
                    Tmp.v2.trns(angle + 90, w);
                    Lines.linePoint(Tmp.v1.x + Tmp.v2.x, Tmp.v1.y + Tmp.v2.y);
                }
            }
            Lines.endLine(true);
        }



        Draw.reset();
        Draw.z(pz);
    }
}
