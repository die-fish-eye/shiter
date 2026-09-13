package myfactorygroupmod;

import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.blocks.production.Drill;

public class GroupDrillBuild extends Drill.DrillBuild {

    public GroupDrillBuild(Drill drill) {
        drill.super();
    }

    @Override
    public void updateTile() {
        GroupSupport.redirectModules(this);
        super.updateTile();
        GroupSupport.extraDump(this);
    }

    @Override
    public boolean shouldConsume(){
        FactoryGroup g = GroupManager.getGroup(this);
        if (g == null || g.members.size <= 0) return super.shouldConsume();

        // 只关心自己挖到的那种矿是否堆满
        if (dominantItem == null) return false;
        int cap = block.itemCapacity * g.members.size;
        return items.get(dominantItem) < cap && enabled;
    }

    @Override
    public int getMaximumAccepted(Item item) {
        return GroupSupport.getMaxAccepted(this, item);
    }

    @Override
    public boolean acceptLiquid(Building source, Liquid liquid) {
        if (GroupManager.getGroup(this) != null) {
            return GroupSupport.acceptLiquid(this, source, liquid);
        }
        return super.acceptLiquid(source, liquid);
    }

    @Override
    public boolean dump(Item item) {
        if (GroupManager.getGroup(this) != null) {
            return GroupSupport.dumpShared(this, item);
        }
        return super.dump(item);
    }

    @Override
    public Seq<Building> getPowerConnections(Seq<Building> out) {
        super.getPowerConnections(out);
        return GroupSupport.getPowerConnections(this, out);
    }

    @Override
    public void display(Table table) {
        super.display(table);
        GroupSupport.displayGroup(this, table);
    }
}