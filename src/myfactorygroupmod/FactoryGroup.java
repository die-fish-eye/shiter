package myfactorygroupmod;

import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.modules.ItemModule;
import mindustry.world.modules.LiquidModule;

public class FactoryGroup {
    public final ObjectSet<Building> members = new ObjectSet<>();
    public final ItemModule sharedItems = new ItemModule();
    public final LiquidModule sharedLiquids = new LiquidModule();

    private String cachedComposition = "";
    private int cachedMemberCount = -1;

    private Set<Item> cachedOutputs = Collections.emptySet();
    private int cachedOutputsMemberCount = -1;

    public boolean contains(Building b) { return members.contains(b); }
    public void add(Building b) { members.add(b); }
    public void remove(Building b) { members.remove(b); }
    public boolean isEmpty() { return members.size == 0; }

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
                sb.append(e.key.localizedName).append("×").append(e.value);
                first = false;
            }
            cachedComposition = sb.toString();
            cachedMemberCount = members.size;
        }
        return cachedComposition;
    }

    /** 群内所有工厂产物的并集 */
    public Set<Item> getSharedOutputs() {
        if (cachedOutputsMemberCount != members.size) {
            Set<Item> outs = new HashSet<>();
            for (Building b : members) {
                if (b.block instanceof GenericCrafter gc) {
                    if (gc.outputItem != null) outs.add(gc.outputItem.item);
                    if (gc.outputItems != null) {
                        for (ItemStack s : gc.outputItems) outs.add(s.item);
                    }
                }
            }
            cachedOutputs = outs;
            cachedOutputsMemberCount = members.size;
        }
        return cachedOutputs;
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