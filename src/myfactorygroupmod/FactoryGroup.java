package myfactorygroupmod;

import arc.struct.ObjectSet;
import mindustry.gen.Building;
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

    /** 群是否已空 */
    public boolean isEmpty() {
        return members.size == 0;
    }

    /** 统计群内工厂的种类和数量 */
    public String describeComposition() {
        // 按方块名称分组统计
        arc.struct.ObjectMap<String, Integer> counts = new arc.struct.ObjectMap<>();
        for (Building b : members) {
            String name = b.block.localizedName;
            counts.put(name, counts.get(name, 0) + 1);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("工厂群包含 ").append(members.size).append(" 个工厂：\n");
        for (arc.struct.ObjectMap.Entry<String, Integer> entry : counts) {
            sb.append("  · ").append(entry.key).append(" × ").append(entry.value).append("\n");
        }
        return sb.toString();
    }
}