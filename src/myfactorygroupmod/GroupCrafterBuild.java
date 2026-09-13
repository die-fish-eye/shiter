package myfactorygroupmod;

import arc.scene.ui.layout.Table;
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
        FactoryGroup g = GroupManager.getGroup(this);
        if (g != null) {
            items = g.sharedItems;
            if (block.hasLiquids) liquids = g.sharedLiquids;
        }

        super.updateTile();

        // 额外 dump，突破原版 dumpTime 节流
        if (g != null && items.total() > 0 && block instanceof GenericCrafter gc
                && gc.outputItems != null) {
            for (ItemStack out : gc.outputItems) {
                for (int i = 0; i < 8; i++) {
                    if (!dump(out.item)) break;
                }
            }
        }
    }

    @Override
    public boolean acceptItem(Building source, Item item) {
        FactoryGroup g = GroupManager.getGroup(this);
        if (g != null) {
            items = g.sharedItems;
            return items.get(item) < getMaximumAccepted(item);
        }
        return super.acceptItem(source, item);
    }

    @Override
    public int getMaximumAccepted(Item item) {
        FactoryGroup g = GroupManager.getGroup(this);
        if (g == null || g.members.size <= 0) return super.getMaximumAccepted(item);
        return block.itemCapacity * g.members.size;
    }

    @Override
    public boolean acceptLiquid(Building source, Liquid liquid) {
        FactoryGroup g = GroupManager.getGroup(this);
        if (g != null) {
            if (block.hasLiquids) liquids = g.sharedLiquids;
            return block.hasLiquids
                && liquids.get(liquid) < block.liquidCapacity * g.members.size;
        }
        return super.acceptLiquid(source, liquid);
    }

    @Override
    public void display(Table table) {
        super.display(table);

        FactoryGroup group = GroupManager.getGroup(this);
        if (group == null || group.members.size <= 1) return;

        table.row();
        table.add("[accent]── 工厂群 ──[]").left().padTop(6f).row();
        table.add("[lightgray]成员: []" + group.members.size).left().row();
    }
}