package myfactorygroupmod;

import arc.graphics.g2d.TextureRegion;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.blocks.production.GenericCrafter;

public class GroupCrafterBuild extends GenericCrafter.GenericCrafterBuild {

    public GroupCrafterBuild(GenericCrafter crafter) {
        crafter.super();
    }

    @Override
    public void created() {
        super.created();
        items = new SharedItemModule(this);
        if (block.hasLiquids) liquids = new SharedLiquidModule(this);
    }

    @Override
    public void updateTile() {
        if (!(items instanceof SharedItemModule)) items = new SharedItemModule(this);
        if (block.hasLiquids && !(liquids instanceof SharedLiquidModule)) {
            liquids = new SharedLiquidModule(this);
        }
        super.updateTile();
    }

    // ===== 物品共享 =====

    /** 群内所有物品一律接收，不受原版配方限制 */
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

    // ===== 液体共享 =====

    @Override
    public boolean acceptLiquid(Building source, Liquid liquid) {
        FactoryGroup myGroup = GroupManager.getGroup(this);
        FactoryGroup srcGroup = source == null ? null : GroupManager.getGroup(source);
        if (myGroup != null && srcGroup != null && myGroup == srcGroup) {
            return block.hasLiquids && liquids.get(liquid) < block.liquidCapacity * myGroup.members.size;
        }
        return super.acceptLiquid(source, liquid);
    }

    // ===== UI =====

    @Override
    public void display(Table table) {
        super.display(table);

        FactoryGroup group = GroupManager.getGroup(this);
        if (group == null || group.members.size <= 1) return;

        table.row();
        table.add("[accent]── 工厂群 ──[]").left().padTop(6f).row();
        table.add("[lightgray]成员总数: []" + group.members.size).left().row();

        ObjectMap<Block, Integer> counts = new ObjectMap<>();
        for (Building b : group.members) {
            counts.put(b.block, counts.get(b.block, 0) + 1);
        }
        for (ObjectMap.Entry<Block, Integer> e : counts) {
            table.add(buildRow(e.key.uiIcon, e.key.localizedName + " × " + e.value))
                 .left().row();
        }

        if (group.sharedItems.total() > 0) {
            table.add("[accent]共享物品[]").left().padTop(4f).row();
            for (Item item : mindustry.Vars.content.items()) {
                int amount = group.sharedItems.get(item);
                if (amount > 0) {
                    table.add(buildRow(item.uiIcon, item.localizedName + ": " + amount))
                         .left().row();
                }
            }
        }

        if (group.sharedLiquids.total() > 0) {
            table.add("[accent]共享液体[]").left().padTop(4f).row();
            for (Liquid liquid : mindustry.Vars.content.liquids()) {
                float amount = group.sharedLiquids.get(liquid);
                if (amount > 0) {
                    table.add(buildRow(liquid.uiIcon, liquid.localizedName + ": "
                        + (int) amount)).left().row();
                }
            }
        }
    }

    private Table buildRow(TextureRegion icon, String text) {
        Table row = new Table();
        row.left();
        row.image(icon).size(20f).padRight(6f);
        Label label = new Label(text);
        label.setEllipsis(true);
        label.setWrap(false);
        row.add(label).left().maxWidth(180f);
        return row;
    }
}