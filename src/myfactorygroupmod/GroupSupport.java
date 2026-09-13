package myfactorygroupmod;

import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.Liquid;

public final class GroupSupport {

    private GroupSupport() {}

    /** 每帧把 items/liquids 强制指向共享池 */
    public static void redirectModules(Building self) {
        FactoryGroup g = GroupManager.getGroup(self);
        if (g != null) {
            self.items = g.sharedItems;
            if (self.block.hasLiquids) self.liquids = g.sharedLiquids;
        }
    }

    /** 额外 dump，输出群内所有工厂的产物并集 */
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
        return self.block.hasLiquids
            && self.liquids.get(liquid) < self.block.liquidCapacity * g.members.size;
    }

    public static boolean dumpShared(Building self, Item item) {
    if (!self.block.hasItems || self.items == null || self.items.total() == 0
            || self.proximity.size == 0) return false;
    if (item != null && !self.items.has(item)) return false;

    FactoryGroup myG = GroupManager.getGroup(self);
    // 只允许 dump 群共享产物
    java.util.Set<Item> allowed = myG != null ? myG.getSharedOutputs() : null;

    int dump = self.cdump;
    var allItems = mindustry.Vars.content.items();
    int itemSize = allItems.size;

    if (item == null) {
        for (int i = 0; i < self.proximity.size; i++) {
            Building other = self.proximity.get((i + dump) % self.proximity.size);
            FactoryGroup otherG = GroupManager.getGroup(other);
            if (myG != null && otherG == myG) {
                self.incrementDump(self.proximity.size);
                continue;
            }

            for (int ii = 0; ii < itemSize; ii++) {
                if (!self.items.has(ii)) continue;
                Item it = allItems.get(ii);
                if (allowed != null && !allowed.contains(it)) continue;
                if (other.acceptItem(self, it) && self.canDump(other, it)) {
                    other.handleItem(self, it);
                    self.items.remove(it, 1);
                    self.incrementDump(self.proximity.size);
                    return true;
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
                self.incrementDump(self.proximity.size);
            }
        } else {
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

    public static Seq<Building> getPowerConnections(Building self, Seq<Building> out) {
        if (self.power == null) return out;
        FactoryGroup g = GroupManager.getGroup(self);
        if (g != null) {
            Seq<Building> snapshot = g.members.toSeq();
            for (Building b : snapshot) {
                if (b != self && b.power != null && b.isValid() && !out.contains(b)) {
                    out.add(b);
                }
            }
        }
        return out;
    }

    /** 悬停面板里显示群信息（成员数 + 组成） */
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