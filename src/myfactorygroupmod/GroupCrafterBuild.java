package myfactorygroupmod;

import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.blocks.production.GenericCrafter;

public class GroupCrafterBuild extends GenericCrafter.GenericCrafterBuild {

    public GroupCrafterBuild(GenericCrafter crafter) {
        crafter.super();
    }

    @Override
    public void updateTile() {
        // ★ 每帧强制 items/liquids 指向共享池
        //   原版 create() 会重置 items 为新的本地模块，导致 dump 时"加共享池、减本地"
        FactoryGroup g = GroupManager.getGroup(this);
        if (g != null) {
            items = g.sharedItems;
            if (block.hasLiquids) liquids = g.sharedLiquids;
        }
        super.updateTile();
    }

    @Override
    public boolean acceptItem(Building source, Item item) {
        FactoryGroup g = GroupManager.getGroup(this);
        if (g != null) {
            items = g.sharedItems;  // 同样强制指向
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

        Label comp = new Label("[lightgray]组成: []" + group.getCompositionString());
        comp.setWrap(true);
        table.add(comp).left().width(220f).row();
    }
}