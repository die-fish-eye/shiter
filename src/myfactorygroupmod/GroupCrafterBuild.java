package myfactorygroupmod;

import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
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
        // 先替换 items / liquids，再走原版逻辑，避免 super 里的 dump 读到本地物品
        FactoryGroup g = GroupManager.getGroup(this);
        if (g != null) {
            if (items != g.sharedItems) items = g.sharedItems;
            if (block.hasLiquids && liquids != g.sharedLiquids) liquids = g.sharedLiquids;
        }
        super.updateTile();
    }

    @Override
    public boolean acceptItem(Building source, Item item) {
        FactoryGroup myGroup = GroupManager.getGroup(this);
        if (myGroup != null) {
            if (items != myGroup.sharedItems) items = myGroup.sharedItems;
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
        if (myGroup != null) {
            if (liquids != myGroup.sharedLiquids) liquids = myGroup.sharedLiquids;
            return block.hasLiquids
                && liquids.get(liquid) < block.liquidCapacity * myGroup.members.size;
        }
        return super.acceptLiquid(source, liquid);
    }

    private boolean isOutput(Item item) {
        if (block.outputItem != null && block.outputItem.item == item) return true;
        if (block.outputItems != null) {
            for (ItemStack s : block.outputItems) {
                if (s.item == item) return true;
            }
        }
        return false;
    }

    @Override
    public boolean dump() {
        if (items.total() <= 0) return false;

        FactoryGroup myGroup = GroupManager.getGroup(this);
        boolean dumped = false;

        for (int i = 0; i < Vars.content.items().size; i++) {
            Item item = Vars.content.item(i);
            if (items.get(item) <= 0) continue;
            if (!isOutput(item)) continue;

            for (Building b : proximity) {
                if (b == this) continue;
                // 目标和我们共享同一个 items 对象 → 跳过
                if (b.items == this.items) continue;
                // 同群 → 跳过
                FactoryGroup otherGroup = GroupManager.getGroup(b);
                if (myGroup != null && otherGroup == myGroup) continue;

                if (b.acceptItem(this, item)) {
                    b.handleItem(this, item);
                    items.remove(item, 1);   // 直接移除，绝不调 offload
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

        Label comp = new Label("[lightgray]组成: []" + group.getCompositionString());
        comp.setWrap(true);
        table.add(comp).left().width(220f).row();
    }
}