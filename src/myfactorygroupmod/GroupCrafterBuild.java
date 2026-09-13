package myfactorygroupmod;

import arc.graphics.Color;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.ui.Bar;
import mindustry.world.blocks.production.GenericCrafter;

public class GroupCrafterBuild extends GenericCrafter.GenericCrafterBuild {

    private Bar liquidBar;

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
            for (int i = 0; i < 3; i++) {
                if (!dump()) break;
            }
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

    @Override
    public boolean dump() {
        if (items.total() <= 0) return false;

        boolean dumped = false;
        for (int i = 0; i < Vars.content.items().size; i++) {
            Item item = Vars.content.item(i);
            if (items.get(item) <= 0) continue;

            for (Building b : proximity) {
                if (b == this) continue;
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
        table.add("[lightgray]成员: []" + group.members.size
            + "   [lightgray]组成: []" + group.getCompositionString()).left().row();

        if (block.hasLiquids) {
            if (liquidBar == null) {
                liquidBar = new Bar(
                    () -> "[lightgray]共享液体[]",
                    () -> Color.royal,
                    () -> {
                        FactoryGroup g = GroupManager.getGroup(this);
                        if (g == null) return 0f;
                        float cap = block.liquidCapacity * g.members.size;
                        return cap > 0
                            ? Math.min(1f, g.sharedLiquids.currentAmount() / cap)
                            : 0f;
                    }
                );
            }
            table.add(liquidBar).width(200f).height(20f).padTop(4f).row();
        }
    }
}