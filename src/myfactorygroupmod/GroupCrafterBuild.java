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

    public GroupCrafterBuild(GenericCrafter crafter) {
        crafter.super();
    }

    // ===== 物品接收：同群一律接收 =====
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

    // ===== 液体接收 =====
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

    // ===== dump：一帧内把所有物品都尝试扔出去，不再 return =====
    @Override
    public void dump() {
        if (items.total() <= 0) return;

        for (int i = 0; i < Vars.content.items().size; i++) {
            Item item = Vars.content.item(i);
            if (items.get(item) <= 0) continue;
            if (!canDump(item)) continue;

            for (Building b : proximity) {
                if (b.acceptItem(this, item)) {
                    int removed = offload(item);
                    if (removed > 0) {
                        b.handleItem(this, item);
                        break;
                    }
                }
            }
        }
    }

    // ===== UI =====
    @Override
    public void display(Table table) {
        super.display(table);

        FactoryGroup group = GroupManager.getGroup(this);
        if (group == null || group.members.size <= 1) return;

        table.row();
        table.add("[accent]── 工厂群 ──[]").left().padTop(6f).row();
        table.add("[lightgray]成员: []" + group.members.size
            + "   [lightgray]组成: []" + group.getCompositionString()).left().row();

        // 液体共享条
        if (block.hasLiquids) {
            float cap = block.liquidCapacity * group.members.size;
            if (cap > 0) {
                table.add(new Bar(
                    () -> "[lightgray]共享液体[]",
                    () -> Color.royal,
                    () -> Math.min(1f, group.sharedLiquids.currentAmount() / cap)
                )).width(200f).height(20f).padTop(4f).row();

                for (Liquid liquid : Vars.content.liquids()) {
                    float amount = group.sharedLiquids.get(liquid);
                    if (amount > 0) {
                        table.image(liquid.uiIcon).size(20f).padRight(4f);
                        table.add(liquid.localizedName + ": " + (int) amount)
                             .left().row();
                    }
                }
            }
        }
    }
}