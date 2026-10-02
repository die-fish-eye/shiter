package myfactorygroupmod;

import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import java.util.Set;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.Liquid;

public final class GroupSupport {

    private GroupSupport() {}

    /** getPowerConnections 专用的去重缓冲，复用以避免每次调用分配。 */
    private static final ObjectSet<Building> powerAddedScratch = new ObjectSet<>();
    private static boolean powerAddedBusy;

    public static void redirectModules(Building self) {
        FactoryGroup g = GroupManager.getGroup(self);
        if (g != null) {
            self.items = g.sharedItems;
            if (self.block.hasLiquids) self.liquids = g.sharedLiquids;
        }
    }

    public static void extraDump(Building self) {
        FactoryGroup g = GroupManager.getGroup(self);
        if (g == null || self.items.total() <= 0) return;
        for (Item item : g.getSharedOutputs()) {
            if (self.items.get(item) <= 0) continue;
            for (int i = 0; i < 8; i++) {
                if (!dumpShared(self, item)) break;
            }
        }
    }

    public static boolean acceptItem(Building self, Building source, Item item) {
        FactoryGroup g = GroupManager.getGroup(self);
        if (g == null) return false;
        self.items = g.sharedItems;

        // 同群来源：直接放行
        FactoryGroup srcGroup = source == null ? null : GroupManager.getGroup(source);
        if (srcGroup != g) {
            // 群外来源（传送带等）：只接受群内工厂的原料（O(1) 掩码查询）
            if (!g.acceptsItem(item)) return false;
        }

        return self.items.get(item) < getMaxAccepted(self, item);
    }

    public static int getMaxAccepted(Building self, Item item) {
        FactoryGroup g = GroupManager.getGroup(self);
        if (g == null || g.members.size <= 0) return self.block.itemCapacity;
        return self.block.itemCapacity * g.members.size;
    }

    public static boolean acceptLiquid(Building self, Building source, Liquid liquid) {
        FactoryGroup g = GroupManager.getGroup(self);
        if (g == null) return false;
        if (self.block.hasLiquids) self.liquids = g.sharedLiquids;

        FactoryGroup srcGroup = source == null ? null : GroupManager.getGroup(source);
        if (srcGroup != g) {
            // 群外来源：只接受群内工厂的原料（O(1) 掩码查询）
            if (!g.acceptsLiquid(liquid)) return false;
        }

        return self.block.hasLiquids
            && self.liquids.get(liquid) < self.block.liquidCapacity * g.members.size;
    }

    /** 只向群外建筑 dump 液体，跳过同群，避免共享池自加自减产生数值漂移 */
    public static void dumpLiquidFiltered(Building self, Liquid liquid, float scaling, int outputDir) {
    if (self.liquids == null || self.liquids.get(liquid) <= 0.0001f) return;

    FactoryGroup myG = GroupManager.getGroup(self);
    int dump = self.cdump;

    for (int i = 0; i < self.proximity.size; i++) {
        self.incrementDump(self.proximity.size);
        Building other = self.proximity.get((i + dump) % self.proximity.size);

        if (outputDir != -1 && (outputDir + self.rotation) % 4 != self.relativeTo(other)) continue;

        // 跳过同群
        FactoryGroup otherG = GroupManager.getGroup(other);
        if (myG != null && otherG == myG) continue;

        other = other.getLiquidDestination(self, liquid);
        if (other == null || !other.block.hasLiquids || !self.canDumpLiquid(other, liquid)
                || other.liquids == null) continue;

        // getLiquidDestination 可能返回同群的另一个成员，再查一次
        FactoryGroup afterG = GroupManager.getGroup(other);
        if (myG != null && afterG == myG) continue;

        float ofract = other.liquids.get(liquid) / other.block.liquidCapacity;
        float fract = self.liquids.get(liquid) / self.block.liquidCapacity;

        if (ofract < fract) {
            self.transferLiquid(other, (fract - ofract) * self.block.liquidCapacity / scaling, liquid);
            }
        }
    }

    public static boolean dumpShared(Building self, Item item) {
        if (!self.block.hasItems || self.items == null || self.items.total() == 0
                || self.proximity.size == 0) return false;
        if (item != null && !self.items.has(item)) return false;

        FactoryGroup myG = GroupManager.getGroup(self);
        // 现在整体带缓存，O(1)
        Set<Item> allowed = myG != null ? myG.getSharedOutputs() : null;

        int dump = self.cdump;

        if (item == null) {
            for (int i = 0; i < self.proximity.size; i++) {
                Building other = self.proximity.get((i + dump) % self.proximity.size);
                FactoryGroup otherG = GroupManager.getGroup(other);

                if (myG != null && otherG == myG) {
                    self.incrementDump(self.proximity.size);
                    continue;
                }

                if (allowed != null) {
                    // 常见情况：allowed 只有 1~3 个物品，只遍历候选，
                    // 不再为每个邻居扫描整张物品表
                    for (Item it : allowed) {
                        if (!self.items.has(it)) continue;
                        if (other.acceptItem(self, it) && self.canDump(other, it)) {
                            other.handleItem(self, it);
                            self.items.remove(it, 1);
                            self.incrementDump(self.proximity.size);
                            return true;
                        }
                    }
                } else {
                    var allItems = mindustry.Vars.content.items();
                    int itemSize = allItems.size;
                    for (int ii = 0; ii < itemSize; ii++) {
                        if (!self.items.has(ii)) continue;
                        Item it = allItems.get(ii);
                        if (other.acceptItem(self, it) && self.canDump(other, it)) {
                            other.handleItem(self, it);
                            self.items.remove(it, 1);
                            self.incrementDump(self.proximity.size);
                            return true;
                        }
                    }
                }
                self.incrementDump(self.proximity.size);
            }
        } else {
            if (allowed != null && !allowed.contains(item)) return false;

            for (int i = 0; i < self.proximity.size; i++) {
                Building other = self.proximity.get((i + dump) % self.proximity.size);
                FactoryGroup otherG = GroupManager.getGroup(other);

                if (myG != null && otherG == myG) {
                    self.incrementDump(self.proximity.size);
                    continue;
                }

                if (other.acceptItem(self, item) && self.canDump(other, item)) {
                    other.handleItem(self, item);
                    self.items.remove(item, 1);
                    self.incrementDump(self.proximity.size);
                    return true;
                }
                self.incrementDump(self.proximity.size);
            }
        }
        return false;
    }

    /**
     * 把整组成员并入本建筑的电力连接。
     * <p>
     * 原来是 {@code g.members.toSeq()} + 逐个 {@code out.contains(b)}：
     * 每次调用都分配一个 Seq，而且是 O(成员数²) 的查重 —— 此方法会被
     * PowerGraph 重建、BlockRenderer 以及本模组的 refreshPower 高频调用。
     * 现在用复用缓冲做 O(1) 去重，单次降到 O(成员数)。
     */
    public static Seq<Building> getPowerConnections(Building self, Seq<Building> out) {
        if (self.power == null) return out;
        FactoryGroup g = GroupManager.getGroup(self);
        if (g == null || g.members.size == 0) return out;

        // 极端情况下（例如某个 mod 的 handleItem 又触发了电网重建）会嵌套进入，
        // 这时退化为线性查重，宁慢不乱。
        if (powerAddedBusy) {
            for (Building b : g.members) {
                if (b != self && b.power != null && b.isValid() && !out.contains(b)) {
                    out.add(b);
                }
            }
            return out;
        }

        powerAddedBusy = true;
        try {
            powerAddedScratch.clear();
            for (int i = 0; i < out.size; i++) {
                powerAddedScratch.add(out.get(i));
            }
            for (Building b : g.members) {
                if (b != self && b.power != null && b.isValid() && !powerAddedScratch.contains(b)) {
                    powerAddedScratch.add(b);
                    out.add(b);
                }
            }
        } finally {
            powerAddedBusy = false;
            // 不要长期持有建筑引用，否则被拆除的建筑无法回收
            powerAddedScratch.clear();
        }
        return out;
    }

    public static void displayGroup(Building self, Table table) {
        FactoryGroup group = GroupManager.getGroup(self);
        if (group == null || group.members.size <= 1) return;

        table.row();
        table.add("[accent]── 工厂群 ──[]").left().padTop(6f).row();
        table.add("[lightgray]成员: []" + group.members.size).left().row();

        Label comp = new Label("[lightgray]组成: []" + group.getCompositionString());
        comp.setWrap(true);
        table.add(comp).left().width(220f).row();
    }
}
