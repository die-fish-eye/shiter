package myfactorygroupmod;

import arc.scene.ui.layout.Table;
import mindustry.Vars;
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
        super.updateTile();

        FactoryGroup g = GroupManager.getGroup(this);
        if (g != null) {
            if (items != g.sharedItems) items = g.sharedItems;
            if (block.hasLiquids && liquids != g.sharedLiquids) liquids = g.sharedLiquids;
        }

        if (g != null && g.members.size > 1 && items.total() > 0) {
            dump();
        }
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

    /** 只向"非同群"的建筑输出物品，避免同群互相喂导致增殖 */
    @Override
    public boolean dump() {
        if (items.total() <= 0) return false;

        FactoryGroup myGroup = GroupManager.getGroup(this);
        boolean dumped = false;

        for (int i = 0; i < Vars.content.items().size; i++) {
            Item item = Vars.content.item(i);
            if (items.get(item) <= 0) continue;

            for (Building b : proximity) {
                if (b == this) continue;

                FactoryGroup otherGroup = GroupManager.getGroup(b);
                if (myGroup != null && otherGroup == myGroup) continue;

                if (b.acceptItem(this, item)) {
                    b.handleItem(this, item);
                    offload(item);
                    dumped = true;
                    break;
                }
            }
        }
        return dumped;
    }

    @Override
    public void display(Table table) {
        super.display(table);

        FactoryGroup group = GroupManager.getGroup(this);
        if (group == null || group.members.size <= 1) return;

        table.row();
        table.add("[accent]── 工厂群 ──[]").left().padTop(6f).row();
        table.add("[lightgray]成员: []" + group.members.size).left().row();

        // 种类统计，允许换行
        arc.scene.ui.Label comp = new arc.scene.ui.Label(
            "[lightgray]组成: []" + group.getCompositionString());
        comp.setWrap(true);
        table.add(comp).left().width(220f).row();
    }
}