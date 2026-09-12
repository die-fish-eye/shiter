package myfactorygroupmod;

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

        // 消耗 1 铜，产出 1 铅
        consumeItem(Items.copper, 1);
        outputItem = new ItemStack(Items.lead, 1);

        // 建造需求：10 铜
        requirements(Category.production, ItemStack.with(Items.copper, 10));

        // 关键：指定建筑类型
        buildType = TestFactoryBuild::new;
    }

    public static class TestFactoryBuild extends GenericCrafterBuild {

        @Override
        public void display(Table table) {
            // 先显示原版内容（名称、血条等）
            super.display(table);

            FactoryGroup group = GroupManager.getGroup(this);
            if (group == null || group.members.size <= 1) return;

            // 分割线
            table.row();
            table.add("[accent]── 工厂群 ──[]").left().padTop(6f).row();

            // 成员总数
            table.add("[lightgray]成员总数: []" + group.members.size).left().row();

            // 按方块种类统计
            ObjectMap<Block, Integer> counts = new ObjectMap<>();
            for (Building b : group.members) {
                counts.put(b.block, counts.get(b.block, 0) + 1);
            }
            for (ObjectMap.Entry<Block, Integer> e : counts) {
                table.image(e.key.uiIcon).size(20f).padRight(4f);
                table.add(e.key.localizedName + " × " + e.value).left().row();
            }

            // 共享库存
            if (group.sharedItems.total() > 0) {
                table.add("[accent]共享库存[]").left().padTop(4f).row();
                for (Item item : mindustry.Vars.content.items()) {
                    int amount = group.sharedItems.get(item);
                    if (amount > 0) {
                        table.image(item.uiIcon).size(20f).padRight(4f);
                        table.add(item.localizedName + ": " + amount).left().row();
                    }
                }
            }
        }
    }
}