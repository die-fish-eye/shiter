package myfactorygroupmod;

import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.modules.ItemModule;
import mindustry.world.modules.LiquidModule;

public class FactoryGroup {
    public final ObjectSet<Building> members = new ObjectSet<>();
    public final ItemModule sharedItems = new ItemModule();
    public final LiquidModule sharedLiquids = new LiquidModule();

    private String cachedComposition;
    private int cachedMemberCount = -1;

    public boolean contains(Building building) { return members.contains(building); }
    public void add(Building building) { members.add(building); }
    public void remove(Building building) { members.remove(building); }
    public boolean isEmpty() { return members.size == 0; }

    /** 缓存种类统计字符串，只在成员数量变化时重建 */
    public String getCompositionString() {
        if (cachedMemberCount != members.size) {
            ObjectMap<Block, Integer> counts = new ObjectMap<>();
            for (Building b : members) {
                counts.put(b.block, counts.get(b.block, 0) + 1);
            }
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (ObjectMap.Entry<Block, Integer> e : counts) {
                if (!first) sb.append("  ");
                sb.append(e.key.localizedName).append(" × ").append(e.value);
                first = false;
            }
            cachedComposition = sb.toString();
            cachedMemberCount = members.size;
        }
        return cachedComposition;
    }

    public void absorbItems(FactoryGroup other) {
        for (Item item : Vars.content.items()) {
            int amount = other.sharedItems.get(item);
            if (amount > 0) sharedItems.add(item, amount);
        }
    }

    public void absorbLiquids(FactoryGroup other) {
        for (Liquid liquid : Vars.content.liquids()) {
            float amount = other.sharedLiquids.get(liquid);
            if (amount > 0) sharedLiquids.add(liquid, amount);
        }
    }
}