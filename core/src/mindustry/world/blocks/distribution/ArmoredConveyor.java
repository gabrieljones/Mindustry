package mindustry.world.blocks.distribution;

import arc.math.geom.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

public class ArmoredConveyor extends Conveyor{

    public ArmoredConveyor(String name){
        super(name);
        noSideBlend = true;
    }

    @Override
    public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){
        return (otherblock.outputsItems() && blendsArmored(tile, rotation, otherx, othery, otherrot, otherblock)) ||
            (lookingAt(tile, rotation, otherx, othery, otherblock) && otherblock.hasItems);
    }

    @Override
    public boolean blendsArmored(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){
        Point2 p = Hex.nearby(tile.x, tile.y, rotation);
        Point2 p2 = Hex.nearby(otherx, othery, otherrot);
        return Point2.equals(p.x, p.y, otherx, othery)
            || ((!otherblock.rotatedOutput(otherx, othery, tile) && Edges.getFacingEdge(otherblock, otherx, othery, tile) != null &&
            Edges.getFacingEdge(otherblock, otherx, othery, tile).relativeTo(tile) == rotation) ||
            (otherblock instanceof Conveyor && otherblock.rotatedOutput(otherx, othery, tile) && Point2.equals(p2.x, p2.y, tile.x, tile.y)));
    }

    public class ArmoredConveyorBuild extends ConveyorBuild{
        @Override
        public boolean acceptItem(Building source, Item item){
            return super.acceptItem(source, item) && (source.block instanceof Conveyor || Edges.getFacingEdge(source.tile, tile).relativeTo(tile) == rotation);
        }
    }
}
