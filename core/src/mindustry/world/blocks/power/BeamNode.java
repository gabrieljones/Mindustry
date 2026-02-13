package mindustry.world.blocks.power;

import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import java.util.*;

import static mindustry.Vars.*;

public class BeamNode extends PowerBlock{
    //maximum expected range of any beam node; used for previews
    private static final int maxRange = 30;

    public int range = 5;

    public @Load(value = "@-beam", fallback = "power-beam") TextureRegion laser;
    public @Load(value = "@-beam-end", fallback = "power-beam-end") TextureRegion laserEnd;

    public Color laserColor1 = Color.white;
    public Color laserColor2 = Color.valueOf("ffd9c2");
    public float pulseScl = 7, pulseMag = 0.05f;
    public float laserWidth = 0.4f;

    public BeamNode(String name){
        super(name);
        consumesPower = outputsPower = false;
        drawDisabled = false;
        envEnabled |= Env.space;
        allowDiagonal = false;
        underBullets = true;
        priority = TargetPriority.transport;
    }

    @Override
    public void setBars(){
        super.setBars();

        addBar("power", PowerNode.makePowerBalance());
        addBar("batteries", PowerNode.makeBatteryBalance());
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.powerRange, range, StatUnit.blocks);
    }

    @Override
    public void init(){
        super.init();

        updateClipRadius((range + 1) * tilesize);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        for(int i = 0; i < 6; i++){
            int maxLen = range + size/2;
            Building dest = null;
            int offset = size/2;
            int cx = x, cy = y;

            for(int k = 0; k < 1 + offset; k++){
                Point2 p = Hex.getOffset(cy, i);
                cx += p.x; cy += p.y;
            }

            for(int j = 1 + offset; j <= range + offset; j++){
                var other = world.build(cx, cy);

                //hit insulated wall
                if(other != null && other.isInsulated()){
                    break;
                }

                if(other != null && other.block.hasPower && other.team == Vars.player.team() && !(other.block instanceof PowerNode)){
                    maxLen = j;
                    dest = other;
                    break;
                }

                Point2 p = Hex.getOffset(cy, i);
                cx += p.x; cy += p.y;
            }

            float px = x * tilesize + Angles.trnsx(i * 60, tilesize * size / 2f + 2);
            float py = y * tilesize + Angles.trnsy(i * 60, tilesize * size / 2f + 2);
            float ex = x * tilesize + Angles.trnsx(i * 60, maxLen * tilesize);
            float ey = y * tilesize + Angles.trnsy(i * 60, maxLen * tilesize);

            Drawf.dashLine(Pal.placing, px, py, ex, ey);

            if(dest != null){
                Drawf.square(dest.x, dest.y, dest.block.size * tilesize/2f + 2.5f, 0f);
            }
        }
    }

    @Override
    public void changePlacementPath(Seq<Point2> points, int rotation, boolean diagonal){
        if(!diagonal){
            Placement.calculateNodes(points, this, rotation, (point, other) -> Math.max(Math.abs(point.x - other.x), Math.abs(point.y - other.y)) <= range + size - 1);
        }
    }

    /** Iterates through linked nodes of a block at a tile. All returned buildings are beam nodes. */
    public static void getNodeLinks(Tile tile, Block block, Team team, Cons<Building> others){
        var tree = team.data().buildingTree;

        if(tree == null) return;

        float cx = tile.worldx() + block.offset, cy = tile.worldy() + block.offset, s = block.size * tilesize/2f, r = maxRange * tilesize;

        for(int i = 0; i < 6; i++){
            float dx = Angles.trnsx(i * 60, r), dy = Angles.trnsy(i * 60, r);
            float minX = Math.min(cx, cx + dx) - s, minY = Math.min(cy, cy + dy) - s;
            float maxX = Math.max(cx, cx + dx) + s, maxY = Math.max(cy, cy + dy) + s;
            Tmp.r1.set(minX, minY, maxX - minX, maxY - minY);

            tempBuilds.clear();
            tree.intersect(Tmp.r1, tempBuilds);
            int fi = i;
            Building closest = tempBuilds.min(b -> b instanceof BeamNodeBuild node && node.couldConnect((fi + 3) % 6, block, tile.x, tile.y), b -> b.dst2(cx, cy));
            tempBuilds.clear();
            if(closest != null){
                others.get(closest);
            }
        }
    }

    /** Note that x1 and y1 are expected to be coordinates of the node to draw the beam from. */
    public void drawLaser(float x1, float y1, float x2, float y2, int size1, int size2){
        float w = laserWidth;
        float dst = Mathf.dst(x1, y1, x2, y2) / tilesize;
        float sizeOff = dst * tilesize - (size1 + size2) * tilesize/2f;

        //don't draw lasers for adjacent blocks
        if(dst > 1 + size/2){
            float angle = Angles.angle(x1, y1, x2, y2);
            int dir = Math.round(angle / 60f) % 6;

            float dx = Angles.trnsx(dir * 60, 1f), dy = Angles.trnsy(dir * 60, 1f);
            float poff = tilesize/2f;
            Drawf.laser(laser, laserEnd, x1 + poff*size*dx, y1 + poff*size*dy, x1 + (poff*size + sizeOff) * dx, y1 + (poff*size + sizeOff) * dy, w);
        }
    }

    public class BeamNodeBuild extends Building{
        //current links in cardinal directions
        public Building[] links = new Building[6];
        public Tile[] dests = new Tile[6];
        public int lastChange = -2;

        /** @return whether a beam could theoretically connect with the specified block at a position */
        public boolean couldConnect(int direction, Block target, int targetX, int targetY){
            int offset = -(target.size - 1) / 2;
            int minX = targetX + offset, minY = targetY + offset, maxX = targetX + offset + target.size - 1, maxY = targetY + offset + target.size - 1;

            int rangeOffset = size/2;
            int cx = tile.x, cy = tile.y;

            for(int k=0; k<1+rangeOffset; k++){
                Point2 p = Hex.getOffset(cy, direction);
                cx += p.x; cy += p.y;
            }

            //find first block with power in range
            for(int j = 1 + rangeOffset; j <= range + rangeOffset; j++){
                var other = world.tile(cx, cy);

                if(other == null) return false;

                Point2 p = Hex.getOffset(cy, direction);
                cx += p.x; cy += p.y;

                //hit insulated wall
                if((other.build != null && other.build.isInsulated()) || (other.block().hasPower && other.block().connectedPower && other.team() == team)){
                    return false;
                }

                //within target rectangle
                if(other.x >= minX && other.y >= minY && other.x <= maxX && other.y <= maxY){
                    return true;
                }
            }

            return false;
        }

        @Override
        public void updateTile(){
            //TODO this block technically does not need to update every frame, perhaps put it in a special list.
            if(lastChange != world.tileChanges){
                lastChange = world.tileChanges;
                updateDirections();
            }
        }

        @Override
        public BlockStatus status(){
            float balance = power.graph.getPowerBalance();
            if(balance > 0f) return BlockStatus.active;
            if(balance < 0f && power.graph.getLastPowerStored() > 0) return BlockStatus.noOutput;
            return BlockStatus.noInput;
        }

        @Override
        public void draw(){
            super.draw();

            if(Mathf.zero(Renderer.laserOpacity) || team == Team.derelict) return;

            Draw.z(Layer.power);
            Draw.color(laserColor1, laserColor2, (1f - power.graph.getSatisfaction()) * 0.86f + Mathf.absin(3f, 0.1f));
            Draw.alpha(Renderer.laserOpacity);
            float w = laserWidth + Mathf.absin(pulseScl, pulseMag);

            for(int i = 0; i < 6; i ++){
                if(dests[i] != null && links[i].wasVisible && (!(links[i].block instanceof BeamNode node) ||
                    (links[i].tileX() != tileX() && links[i].tileY() != tileY()) ||
                    (links[i].id > id && range >= node.range) || range > node.range)){

                    float worldDst = Mathf.dst(dests[i].worldx(), dests[i].worldy(), x, y);

                    //don't draw lasers for adjacent blocks
                    if(worldDst > (1 + size/2) * tilesize){
                        float dx = Angles.trnsx(i * 60, 1f), dy = Angles.trnsy(i * 60, 1f);
                        float poff = tilesize/2f;
                        Drawf.laser(laser, laserEnd, x + poff*size*dx, y + poff*size*dy, dests[i].worldx() - poff*dx, dests[i].worldy() - poff*dy, w);
                    }
                }
            }

            Draw.reset();
        }

        @Override
        public void pickedUp(){
            Arrays.fill(links, null);
            Arrays.fill(dests, null);
        }

        public void updateDirections(){
            for(int i = 0; i < 6; i ++){
                var prev = links[i];
                links[i] = null;
                dests[i] = null;
                int offset = size/2;
                int cx = tile.x, cy = tile.y;

                for(int k = 0; k < 1 + offset; k++){
                    Point2 p = Hex.getOffset(cy, i);
                    cx += p.x; cy += p.y;
                }

                //find first block with power in range
                for(int j = 1 + offset; j <= range + offset; j++){
                    var other = world.build(cx, cy);

                    //hit insulated wall
                    if(other != null && other.isInsulated()){
                        break;
                    }

                    //power nodes do NOT play nice with beam nodes, do not touch them as that forcefully modifies their links
                    if(other != null && other.block.hasPower && other.block.connectedPower && other.team == team && !(other.block instanceof PowerNode)){
                        links[i] = other;
                        dests[i] = world.tile(cx, cy);
                        break;
                    }

                    Point2 p = Hex.getOffset(cy, i);
                    cx += p.x; cy += p.y;
                }

                var next = links[i];

                if(next != prev){
                    //unlinked, disconnect and reflow
                    if(prev != null && prev.isAdded()){
                        prev.power.links.removeValue(pos());
                        power.links.removeValue(prev.pos());

                        PowerGraph newgraph = new PowerGraph();
                        //reflow from this point, covering all tiles on this side
                        newgraph.reflow(this);

                        if(prev.power.graph != newgraph){
                            //reflow power for other end
                            PowerGraph og = new PowerGraph();
                            og.reflow(prev);
                        }
                    }

                    //linked to a new one, connect graphs
                    if(next != null){
                        power.links.addUnique(next.pos());
                        next.power.links.addUnique(pos());

                        power.graph.addGraph(next.power.graph);
                    }
                }
            }
        }
    }
}
