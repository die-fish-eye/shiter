package myfactorygroupmod;

import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.world.modules.ItemModule;

public class FactoryGroup {
    /** 群内所有工厂方块 */
    public final ObjectSet<Building> members = new ObjectSet<>();

    /** 共享库存 —— 复用原版 ItemModule */
    public final ItemModule sharedItems = new ItemModule();

    public boolean contains(Building building) {
        return members.contains(building);
    }

    public void add(Building building) {
        members.add(building);
    }

    public void remove(Building building) {
        members.remove(building);
    }

    public boolean isEmpty() {
        return members.size == 0;
    }

    /** 统计群内工厂的种类和数量 */
    public String describeComposition() {
        ObjectMap<String, Integer> counts = new ObjectMap<>();
        for (Building b : members) {
            String name = b.block.localizedName;
            counts.put(name, counts.get(name, 0) + 1);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("工厂群包含 ").append(members.size).append(" 个工厂：\n");
        for (ObjectMap.Entry<String, Integer> entry : counts) {
            sb.append("  · ").append(entry.key).append(" × ").append(entry.value).append("\n");
        }
        return sb.toString();
    }

    /** 合并另一个群的物品到本群（用于群合并） */
    public void absorbItems(FactoryGroup other) {
        for (Item item : Vars.content.items()) {
            int amount = other.sharedItems.get(item);
            if (amount > 0) {
                sharedItems.add(item, amount);
            }
        }
    }
}