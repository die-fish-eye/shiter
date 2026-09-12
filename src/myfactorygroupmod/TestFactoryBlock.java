package myfactorygroupmod;

import arc.graphics.g2d.TextureRegion;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import mindustry.content.Items;
import mindustry.gen.Building;
import mindustry.type.Category;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.blocks.production.GenericCrafter;

public class TestFactoryBlock extends GenericCrafter {

    public TestFactoryBlock() {
        super("test-factory");

        localizedName = "测试工厂";
        description = "用于测试工厂群的工厂";

        size = 2;
        health = 200;
        hasItems = true;
        hasPower = false;
        hasLiquids = false;
        solid = true;
        update = true;
        destructible = true;
        itemCapacity = 20;
        craftTime = 60f;

        consumeItem(Items.copper, 1);
        outputItem = new ItemStack(Items.lead, 1);

        requirements(Category.production, ItemStack.with(Items.copper, 10));

        buildType = TestFactoryBuild::new;
    }

    public class TestFactoryBuild extends GenericCrafterBuild {

        @Override
        public void display(Table table) {
            super.display(table);

            FactoryGroup group = GroupManager.getGroup(this);
            if (group == null || group.members.size <= 1) return;

            table.row();
            table.add("[accent]── 工厂群 ──[]").left().padTop(6f).row();
            table.add("[lightgray]成员总数: []" + group.members.size).left().row();

            // 按方块种类统计
            ObjectMap<Block, Integer> counts = new ObjectMap<>();
            for (Building b : group.members) {
                counts.put(b.block, counts.get(b.block, 0) + 1);
            }
            for (ObjectMap.Entry<Block, Integer> e : counts) {
                table.add(buildRow(e.key.uiIcon, e.key.localizedName + " × " + e.value)).left().row();
            }

            // 共享库存
            if (group.sharedItems.total() > 0) {
                table.add("[accent]共享库存[]").left().padTop(4f).row();
                for (Item item : mindustry.Vars.content.items()) {
                    int amount = group.sharedItems.get(item);
                    if (amount > 0) {
                        table.add(buildRow(item.uiIcon, item.localizedName + ": " + amount)).left().row();
                    }
                }
            }
        }

        /** 图标 + 文本 左对齐的一行 */
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
}