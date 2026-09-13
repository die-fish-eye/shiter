package myfactorygroupmod;

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
    public boolean acceptItem(Building source, Item item) {
        FactoryGroup myGroup = GroupManager.getGroup(this);
        FactoryGroup srcGroup = source == null ? null : GroupManager.getGroup(source);
        if (myGroup != null && srcGroup != null && myGroup == srcGroup) {
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
        FactoryGroup myGroup = GroupManager.getGroup(this);
        FactoryGroup srcGroup = source == null ? null : GroupManager.getGroup(source);
        if (myGroup != null && srcGroup != null && myGroup == srcGroup) {
            return block.hasLiquids
                && liquids.get(liquid) < block.liquidCapacity * myGroup.members.size;
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
        table.add("[lightgray]成员总数: []" + group.members.size).left().row();

        // 工厂种类（用缓存，避免每帧重建）
        table.add(group.getCompositionString()).left().row();

        // 共享物品
        boolean hasItem = false;
        for (Item item : mindustry.Vars.content.items()) {
            if (group.sharedItems.get(item) > 0) { hasItem = true; break; }
        }
        if (hasItem) {
            table.add("[accent]共享物品[]").left().padTop(4f).row();
            for (Item item : mindustry.Vars.content.items()) {
                int amount = group.sharedItems.get(item);
                if (amount > 0) {
                    table.image(item.uiIcon).size(20f).padRight(6f);
                    table.add(item.localizedName + ": " + amount).left().row();
                }
            }
        }

        // 共享液体
        boolean hasLiquid = false;
        for (Liquid liquid : mindustry.Vars.content.liquids()) {
            if (group.sharedLiquids.get(liquid) > 0) { hasLiquid = true; break; }
        }
        if (hasLiquid) {
            table.add("[accent]共享液体[]").left().padTop(4f).row();
            for (Liquid liquid : mindustry.Vars.content.liquids()) {
                float amount = group.sharedLiquids.get(liquid);
                if (amount > 0) {
                    table.image(liquid.uiIcon).size(20f).padRight(6f);
                    table.add(liquid.localizedName + ": " + (int) amount).left().row();
                }
            }
        }
    }
}