package myfactorygroupmod;

import arc.graphics.g2d.TextureRegion;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.world.Block;
import mindustry.world.blocks.production.GenericCrafter;

public class GroupCrafterBuild extends GenericCrafter.GenericCrafterBuild {

    public GroupCrafterBuild(GenericCrafter crafter) {
        crafter.super();
    }

    @Override
    public void created() {
        super.created();
        // ★ super.created() 会重新 new 一个 ItemModule，这里再替换一次
        this.items = new SharedItemModule(this);
    }

    @Override
    public void updateTile() {
        // 保险：万一还有别的逻辑替换了 items，这里兜一次
        if (!(items instanceof SharedItemModule)) {
            items = new SharedItemModule(this);
        }
        super.updateTile();
    }

    @Override
    public int getMaximumAccepted(Item item) {
        FactoryGroup g = GroupManager.getGroup(this);
        if (g == null || g.members.size <= 0) {
            return super.getMaximumAccepted(item);
        }
        return block.itemCapacity * g.members.size;
    }

    @Override
    public void display(Table table) {
        super.display(table);

        FactoryGroup group = GroupManager.getGroup(this);
        if (group == null || group.members.size <= 1) return;

        table.row();
        table.add("[accent]── 群组织 ──[]").left().padTop(6f).row();
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
            table.add("[accent]共享库存[]").left().padTop(4f).row();
            for (Item item : mindustry.Vars.content.items()) {
                int amount = group.sharedItems.get(item);
                if (amount > 0) {
                    table.add(buildRow(item.uiIcon, item.localizedName + ": " + amount))
                         .left().row();
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