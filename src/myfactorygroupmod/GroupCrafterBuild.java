package myfactorygroupmod;

import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.Liquid;
import mindustry.world.blocks.production.GenericCrafter;

public class GroupCrafterBuild extends GenericCrafter.GenericCrafterBuild {

    public GroupCrafterBuild(GenericCrafter crafter) {
        crafter.super();
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
        if (g == null) return super.shouldConsume();

        int cap = block.itemCapacity * g.members.size;
        if (block instanceof GenericCrafter gc && gc.outputItems != null) {
            for (ItemStack out : gc.outputItems) {
                if (items.get(out.item) >= cap) return false;
            }
        }
        if (block.hasLiquids && liquids != null) {
            for (Liquid liq : Vars.content.liquids()) {
                if (liquids.get(liq) >= block.liquidCapacity * g.members.size) return false;
            }
        }
        return enabled;
    }

    @Override
    public boolean acceptItem(Building source, Item item) {
        if (GroupManager.getGroup(this) != null) {
            return GroupSupport.acceptItem(this, source, item);
        }
        return super.acceptItem(source, item);
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