package rrr.content.blocks;

import mindustry.content.Items;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;

public class chaowall extends Block {
    public chaowall() {
        super("rrr-chaowall");   // 方块内部名称，建议加模组前缀
        health = 100;
        size = 1;
        requirements(Category.defense, ItemStack.with(Items.copper, 10));
    }
}