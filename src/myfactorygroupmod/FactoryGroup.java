package myfactorygroupmod;

import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.modules.ItemModule;
import mindustry.world.modules.LiquidModule;

public class FactoryGroup {
    public final ObjectSet<Building> members = new ObjectSet<>();
    public final ItemModule sharedItems = new ItemModule();
    public final LiquidModule sharedLiquids = new LiquidModule();

    /** 墙群共享血量比例（1.0 = 满血）。非墙群不使用此字段。 */
    public float wallHealthFraction = 1f;

    /**
     * 成员变化计数：所有派生缓存都用它做失效判断。
     * 不能用 members.size —— 同一 tick 内“拆掉一个钻头、加上一个工厂”时 size 不变，
     * 但组成已经变了，用 size 当键会读到脏缓存。
     */
    private int version;

    private String cachedComposition = "";
    private int cachedCompositionVersion = -1;

    private Set<Item> cachedOutputs = Collections.emptySet();
    private int cachedOutputsVersion = -1;

    private boolean[] inputItems;
    private boolean[] inputLiquids;
    private int cachedInputsVersion = -1;

    /** 群内所有炮塔弹药类型并集掩码：O(1) 判断某物品是否是群内某炮塔的弹药 */
    private boolean[] turretAmmoMask;
    private int cachedAmmoVersion = -1;

    public boolean contains(Building b) { return members.contains(b); }

    public void add(Building b) {
        if (members.contains(b)) return;
        members.add(b);
        version++;
    }

    public void remove(Building b) {
        if (!members.contains(b)) return;
        members.remove(b);
        version++;
    }

    public boolean isEmpty() { return members.size == 0; }

    /** 清空成员。世界重载时由 GroupManager.reset() 调用。 */
    public void clear() {
        if (members.size > 0) {
            members.clear();
            version++;
        }
    }

    public String getCompositionString() {
        if (cachedCompositionVersion != version) {
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
            cachedCompositionVersion = version;
        }
        return cachedComposition;
    }

    /**
     * 群内可被 dump 的产物集合。
     * - 工厂产物（GenericCrafter/Separator）：总是包含
     * - 钻头产物：仅当群内没有工厂时才包含，否则留给工厂消费
     */
    public Set<Item> getSharedOutputs() {
        if (cachedOutputsVersion != version) {
            Set<Item> outs = new HashSet<>();
            for (Building b : members) {
                Block block = b.block;
                if (block instanceof mindustry.world.blocks.production.GenericCrafter gc) {
                    if (gc.outputItem != null) outs.add(gc.outputItem.item);
                    if (gc.outputItems != null) {
                        for (ItemStack s : gc.outputItems) outs.add(s.item);
                    }
                }
                if (block instanceof mindustry.world.blocks.production.Separator sep) {
                    if (sep.results != null) {
                        for (ItemStack s : sep.results) outs.add(s.item);
                    }
                }
            }

            boolean hasFactory = false;
            for (Building b : members) {
                if (b instanceof GroupCrafterBuild
                        || b instanceof GroupSeparatorBuild
                        || b instanceof GroupAttributeCrafterBuild) {
                    hasFactory = true;
                    break;
                }
            }

            if (!hasFactory) {
                Set<Item> combined = null;
                for (Building b : members) {
                    if (b instanceof mindustry.world.blocks.production.Drill.DrillBuild db
                            && db.dominantItem != null) {
                        if (combined == null) combined = new HashSet<>(outs);
                        combined.add(db.dominantItem);
                    }
                }
                if (combined != null) outs = combined;
            }

            cachedOutputs = outs;
            cachedOutputsVersion = version;
        }
        return cachedOutputs;
    }

    /** 群内是否有建筑把该物品当原料（合并自 Block.itemFilter，O(1) 查询）。 */
    public boolean acceptsItem(Item item) {
        if (item == null) return false;
        int isize = Vars.content.items().size;
        if (cachedInputsVersion != version || inputItems == null || inputItems.length != isize) {
            rebuildInputs();
        }
        return item.id < inputItems.length && inputItems[item.id];
    }

    /** 群内是否有建筑把该液体当原料（合并自 Block.liquidFilter，O(1) 查询）。 */
    public boolean acceptsLiquid(Liquid liquid) {
        if (liquid == null) return false;
        int lsize = Vars.content.liquids().size;
        if (cachedInputsVersion != version || inputLiquids == null || inputLiquids.length != lsize) {
            rebuildInputs();
        }
        return liquid.id < inputLiquids.length && inputLiquids[liquid.id];
    }

    /** 群内是否有炮塔把该物品当弹药（O(1) 掩码查询）。 */
    public boolean acceptsTurretAmmo(Item item) {
        if (item == null) return false;
        int isize = Vars.content.items().size;
        if (cachedAmmoVersion != version || turretAmmoMask == null || turretAmmoMask.length != isize) {
            rebuildAmmoMask();
        }
        return item.id < turretAmmoMask.length && turretAmmoMask[item.id];
    }

    private void rebuildInputs() {
        int isize = Vars.content.items().size;
        int lsize = Vars.content.liquids().size;

        if (inputItems == null || inputItems.length != isize) inputItems = new boolean[isize];
        else Arrays.fill(inputItems, false);

        if (inputLiquids == null || inputLiquids.length != lsize) inputLiquids = new boolean[lsize];
        else Arrays.fill(inputLiquids, false);

        for (Building b : members) {
            if (b == null || b.block == null) continue;

            boolean[] fi = b.block.itemFilter;
            if (fi != null) {
                int n = Math.min(fi.length, isize);
                for (int i = 0; i < n; i++) {
                    if (fi[i]) inputItems[i] = true;
                }
            }

            boolean[] fl = b.block.liquidFilter;
            if (fl != null) {
                int n = Math.min(fl.length, lsize);
                for (int i = 0; i < n; i++) {
                    if (fl[i]) inputLiquids[i] = true;
                }
            }
        }

        cachedInputsVersion = version;
    }

    private void rebuildAmmoMask() {
        int isize = Vars.content.items().size;
        if (turretAmmoMask == null || turretAmmoMask.length != isize) turretAmmoMask = new boolean[isize];
        else Arrays.fill(turretAmmoMask, false);

        for (Building b : members) {
            if (b.block instanceof ItemTurret it) {
                for (Item ammo : it.ammoTypes.keys()) {
                    if (ammo.id < isize) turretAmmoMask[ammo.id] = true;
                }
            }
        }

        cachedAmmoVersion = version;
    }

    /** 合并另一个群的物品：只遍历非零槽位，避免扫全物品表。 */
    public void absorbItems(FactoryGroup other) {
        other.sharedItems.each((item, amount) -> {
            if (amount > 0) sharedItems.add(item, amount);
        });
    }

    /** 合并另一个群的液体：只遍历非零槽位。 */
    public void absorbLiquids(FactoryGroup other) {
        other.sharedLiquids.each((liquid, amount) -> {
            if (amount > 0) sharedLiquids.add(liquid, amount);
        });
    }
}