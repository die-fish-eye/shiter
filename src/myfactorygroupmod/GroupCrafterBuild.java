package myfactorygroupmod;

import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.Liquid;
import mindustry.type.LiquidStack;
import mindustry.world.blocks.production.GenericCrafter;

public class GroupCrafterBuild extends GenericCrafter.GenericCrafterBuild implements GroupFactoryBuild {

    private FactoryGroup groupRef;

    @Override
    public FactoryGroup fgmGroup() {
        return groupRef;
    }

    @Override
    public void fgmGroup(FactoryGroup group) {
        groupRef = group;
    }

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

        // 只检查本工厂的产物液体是否都满，而不是共享池里任意一种液体
        // 原版语义：outputLiquids 全部达到容量才停工
        if (block instanceof GenericCrafter gc && gc.outputLiquids != null && liquids != null) {
            float liqCap = block.liquidCapacity * g.members.size;
            boolean allFull = true;
            for (LiquidStack ls : gc.outputLiquids) {
                if (liquids.get(ls.liquid) < liqCap - 0.001f) {
                    allFull = false;
                    break;
                }
            }
            if (allFull) return false;
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