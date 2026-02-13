package mindustry.world.blocks.distribution;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class DirectionBridge extends Block{
    private static BuildPlan otherReq;
    private int otherDst = 0;

    public @Load("@-bridge") TextureRegion bridgeRegion;
    public @Load("@-bridge-bottom") TextureRegion bridgeBotRegion;
    public @Load("@-bridge-liquid") TextureRegion bridgeLiquidRegion;
    public @Load("@-arrow") TextureRegion arrowRegion;
    public @Load("@-dir") TextureRegion dirRegion;

    public int range = 4;

    public DirectionBridge(String name){
        super(name);
        update = true;
        solid = true;
        rotate = true;
        group = BlockGroup.transportation;
        noUpdateDisabled = true;
        priority = TargetPriority.transport;
        envEnabled = Env.space | Env.terrestrial | Env.underwater;
        drawArrow = false;
        allowDiagonal = false;
        regionRotated1 = 1;
    }

    @Override
    public void init(){
        updateClipRadius((range + 0.5f) * tilesize);
        super.init();
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        Draw.rect(region, plan.drawx(), plan.drawy());
        Draw.rect(dirRegion, plan.drawx(), plan.drawy(), plan.rotation * 90);
    }

    @Override
    public void drawPlanConfigTop(BuildPlan plan, Eachable<BuildPlan> list){
        otherReq = null;
        otherDst = range;

        list.each(other -> {
            if(other.block == this && plan != other){
                //check if 'other' is reachable from 'plan' in 'plan.rotation'
                int cx = plan.x, cy = plan.y;
                for(int i = 1; i <= range; i++){
                    Point2 next = Hex.nearby(cx, cy, plan.rotation);
                    cx = next.x;
                    cy = next.y;

                    if(other.x == cx && other.y == cy){
                        if(i <= otherDst){
                            otherReq = other;
                            otherDst = i;
                        }
                        break;
                    }
                }
            }
        });

        if(otherReq != null){
            drawBridge(plan.rotation, plan.drawx(), plan.drawy(), otherReq.drawx(), otherReq.drawy(), null);
        }
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region, dirRegion};
    }

    @Override
    public void changePlacementPath(Seq<Point2> points, int rotation){
        Placement.calculateNodes(points, this, rotation, (point, other) -> positionsValid(point.x, point.y, other.x, other.y));
    }

    public void drawPlace(int x, int y, int rotation, boolean valid, boolean line){
        int length = range;

        if(line){
            //find input links
            for(int d = 0; d < 6; d++){
                if(d == (rotation + 3) % 6) continue;

                //look in opposite direction of d
                int lookDir = (d + 3) % 6;
                int cx = x, cy = y;

                for(int i = 1; i <= range; i++){
                    Point2 p = Hex.nearby(cx, cy, lookDir);
                    cx = p.x;
                    cy = p.y;

                    Tile other = world.tile(cx, cy);

                    if(other != null && other.build instanceof DirectionBridgeBuild build && build.block == this && build.team == player.team()){
                        if(build.rotation == d){
                            var from = other.build;

                            //draw from 'from' to 'this'
                            //Since we don't have exact coordinates of intermediate steps easily here without re-looping,
                            //we just draw a straight line between the two world positions.

                            Drawf.dashLine(Pal.place, from.x, from.y, Hex.worldX(x, y), Hex.worldY(y));
                            Drawf.square(from.x, from.y, from.block.size * tilesize/2f + 2.5f, 0f, Pal.place);
                        }
                        //always stop when a bridge is encountered, as it blocks incoming bridges from this side
                        break;
                    }
                }
            }
        }

        Building found = null;
        int cx = x, cy = y;

        //find the output link
        for(int i = 1; i <= range; i++){
            Point2 p = Hex.nearby(cx, cy, rotation);
            cx = p.x;
            cy = p.y;

            Tile other = world.tile(cx, cy);

            if(other != null && other.build instanceof DirectionBridgeBuild build && build.block == this && build.team == player.team()){
                length = i;
                found = other.build;
                break;
            }
        }

        if(line || found != null){
            float endX = (found != null) ? found.x : Hex.worldX(cx, cy);
            float endY = (found != null) ? found.y : Hex.worldY(cy);

            Drawf.dashLine(Pal.placing, Hex.worldX(x, y), Hex.worldY(y), endX, endY);
        }

        if(found != null){
            if(line){
                Drawf.square(found.x, found.y, found.block.size * tilesize/2f + 2.5f, 0f);
            }else{
                Drawf.square(found.x, found.y, 2f);
            }
        }
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);

        drawPlace(x, y, rotation, valid, true);
    }

    public void drawBridge(int rotation, float x1, float y1, float x2, float y2, @Nullable Color liquidColor){
        Draw.alpha(Renderer.bridgeOpacity);
        float
        angle = Angles.angle(x1, y1, x2, y2),
        cx = (x1 + x2)/2f,
        cy = (y1 + y2)/2f,
        len = Mathf.dst(x1, y1, x2, y2) - size * tilesize;

        Draw.rect(bridgeRegion, cx, cy, len, bridgeRegion.height * bridgeRegion.scl(), angle);
        if(liquidColor != null){
            Draw.color(liquidColor, liquidColor.a * Renderer.bridgeOpacity);
            Draw.rect(bridgeLiquidRegion, cx, cy, len, bridgeLiquidRegion.height * bridgeLiquidRegion.scl(), angle);
            Draw.color();
            Draw.alpha(Renderer.bridgeOpacity);
        }
        if(bridgeBotRegion.found()){
            Draw.color(0.4f, 0.4f, 0.4f, 0.4f * Renderer.bridgeOpacity);
            Draw.rect(bridgeBotRegion, cx, cy, len, bridgeBotRegion.height * bridgeBotRegion.scl(), angle);
            Draw.reset();
        }
        Draw.alpha(Renderer.bridgeOpacity);

        for(float i = 6f; i <= len + size * tilesize - 5f; i += 5f){
            Draw.rect(arrowRegion, x1 + Angles.trnsx(angle, i), y1 + Angles.trnsy(angle, i), angle);
        }

        Draw.reset();
    }

    public boolean positionsValid(int x1, int y1, int x2, int y2){
        //check if x2/y2 is reachable from x1/y1 by iterating 6 directions
        for(int i = 0; i < 6; i++){
            int cx = x1, cy = y1;
            for(int j = 1; j <= range; j++){
                Point2 p = Hex.nearby(cx, cy, i);
                cx = p.x;
                cy = p.y;
                if(cx == x2 && cy == y2) return true;
            }
        }
        return false;
    }

    public class DirectionBridgeBuild extends Building{
        public DirectionBridgeBuild[] occupied = new DirectionBridgeBuild[6];
        public @Nullable DirectionBridgeBuild lastLink;

        @Override
        public void draw(){
            Draw.rect(block.region, x, y);
            Draw.rect(dirRegion, x, y, rotdeg());
            var link = findLink();
            if(link != null){
                Draw.z(Layer.power - 1);
                drawBridge(rotation, x, y, link.x, link.y, null);
            }
        }

        @Override
        public void drawSelect(){
            drawPlace(tile.x, tile.y, rotation, true, false);
            //draw incoming bridges
            for(int dir = 0; dir < 6; dir++){
                if(dir != rotation){
                    Building found = occupied[(dir + 3) % 6];

                    if(found != null){
                        //draw line from found to this
                        Drawf.dashLine(Pal.place, found.x, found.y, x, y);
                        Drawf.square(found.x, found.y, 2f, 45f, Pal.place);
                    }
                }
            }
        }

        @Nullable
        public DirectionBridgeBuild findLink(){
            int cx = tile.x, cy = tile.y;
            for(int i = 1; i <= range; i++){
                Point2 p = Hex.nearby(cx, cy, rotation);
                cx = p.x;
                cy = p.y;

                Tile other = world.tile(cx, cy);
                if(other != null && other.build instanceof DirectionBridgeBuild build && build.block == DirectionBridge.this && build.team == team){
                    return build;
                }
            }
            return null;
        }
    }
}
