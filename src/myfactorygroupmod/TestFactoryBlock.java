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

        consumeItem(Items.copper, 1);
        outputItem = new ItemStack(Items.lead, 1);

        requirements(Category.production, ItemStack.with(Items.copper, 10));

        // 关键：非静态内部类的方法引用会自动捕获 this
        buildType = TestFactoryBuild::new;
    }

    // 注意：这里不是 static！
    public class TestFactoryBuild extends GenericCrafterBuild {

        @Override
        public void display(Table table) {
            super.display(table);

            FactoryGroup group = GroupManager.getGroup(this);
            if (group == null || group.members.size <= 1) return;

            table.row();
            table.add("[accent]── 工厂群 ──[]").left().padTop(6f).row();
            table.add("[lightgray]成员总数: []" + group.members.size).left().row();

            // 按方块种类统计（名字过长时用省略号）
            ObjectMap<Block, Integer> counts = new ObjectMap<>();
            for (Building b : group.members) {
            counts.put(b.block, counts.get(b.block, 0) + 1);
}
            for (ObjectMap.Entry<Block, Integer> e : counts) {
            table.image(e.key.uiIcon).size(20f).padRight(4f);

            arc.scene.ui.Label nameLabel = new arc.scene.ui.Label(e.key.localizedName + " × " + e.value);
            nameLabel.setEllipsis(true);
            nameLabel.setWrap(false);
            table.add(nameLabel).left().width(160f).row();
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