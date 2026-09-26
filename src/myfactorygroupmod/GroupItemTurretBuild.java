package myfactorygroupmod;

import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Log;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.blocks.defense.turrets.Turret;

public class GroupItemTurretBuild extends ItemTurret.ItemTurretBuild {

    // === 反射缓存：原版 ItemEntry 的构造器和 item 字段 ===
    private static Constructor<?> itemEntryCtor;
    private static Field itemEntryItemField;

    static {
        try {
            Class<?> cls = Class.forName(
                "mindustry.world.blocks.defense.turrets.ItemTurret$ItemEntry");
            itemEntryCtor = cls.getDeclaredConstructor(ItemTurret.class, Item.class, int.class);
            itemEntryCtor.setAccessible(true);
            itemEntryItemField = cls.getField("item");
        } catch (Throwable t) {
            Log.warn("[fgm] 无法反射 ItemEntry: @", t.getMessage());
        }
    }

    public GroupItemTurretBuild(ItemTurret turret) {
        turret.super();
    }

    /** 通过反射创建原版 ItemEntry */
    private Turret.AmmoEntry makeEntry(Item item, int amount) {
        if (itemEntryCtor == null) return null;
        try {
            return (Turret.AmmoEntry) itemEntryCtor.newInstance((ItemTurret) block, item, amount);
        } catch (Throwable t) {
            return null;
        }
    }

    /** 读取 entry 的 item 字段 */
    private Item getEntryItem(Turret.AmmoEntry entry) {
        if (entry == null || itemEntryItemField == null) return null;
        try {
            return (Item) itemEntryItemField.get(entry);
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public void updateTile() {
        GroupSupport.redirectModules(this);
        super.updateTile();
    }

    // ===== 物品接收 =====

    @Override
    public boolean acceptItem(Building source, Item item) {
        FactoryGroup g = GroupManager.getGroup(this);
        if (g == null) return super.acceptItem(source, item);
        if (((ItemTurret) block).ammoTypes.get(item) == null) return false;
        items = g.sharedItems;
        return items.get(item) < getMaximumAccepted(item);
    }

    @Override
    public int getMaximumAccepted(Item item) {
        return GroupSupport.getMaxAccepted(this, item);
    }

    @Override
    public void handleItem(Building source, Item item) {
        FactoryGroup g = GroupManager.getGroup(this);
        if (g == null) {
            super.handleItem(source, item);
            return;
        }
        ItemTurret turret = (ItemTurret) block;
        BulletType type = turret.ammoTypes.get(item);
        if (type == null) return;

        items = g.sharedItems;
        items.add(item, 1);

        // 更新或添加 ammo 队列条目（用原版 ItemEntry）
        for (int i = 0; i < ammo.size; i++) {
            Turret.AmmoEntry entry = ammo.get(i);
            Item entryItem = getEntryItem(entry);
            if (entryItem == item) {
                entry.amount += (int) type.ammoMultiplier;
                ammo.swap(i, ammo.size - 1);
                return;
            }
        }
        Turret.AmmoEntry newEntry = makeEntry(item, (int) type.ammoMultiplier);
        if (newEntry != null) ammo.add(newEntry);
    }

    // ===== 弹药消费 =====

    @Override
    public boolean hasAmmo() {
        FactoryGroup g = GroupManager.getGroup(this);
        if (g == null) return super.hasAmmo();
        if (!canConsume()) return false;
        if (ammo.size == 0) return false;
        if (cheating()) return true;

        Turret.AmmoEntry entry = ammo.peek();
        Item cur = getEntryItem(entry);
        if (cur == null) return false;

        int use = ((ItemTurret) block).ammoPerShot;
        if (entry.amount >= use) return true;

        // 弹夹不足时看共享池
        items = g.sharedItems;
        return items.get(cur) >= 1;
    }

    @Override
    public BulletType useAmmo() {
        FactoryGroup g = GroupManager.getGroup(this);
        if (g == null) return super.useAmmo();
        if (cheating()) return peekAmmo();
        if (ammo.size == 0) return null;

        Turret.AmmoEntry entry = ammo.peek();
        Item cur = getEntryItem(entry);
        if (cur == null) return null;
        BulletType type = ((ItemTurret) block).ammoTypes.get(cur);
        if (type == null) return null;

        int use = ((ItemTurret) block).ammoPerShot;

        // 弹夹不足，从共享池扣 1 个物品重新填满
        if (entry.amount < use) {
            items = g.sharedItems;
            if (items.get(cur) < 1) return null;
            items.remove(cur, 1);
            entry.amount += (int) type.ammoMultiplier;
        }

        if (entry.amount < use) return null;

        entry.amount -= use;
        totalAmmo = Math.max(totalAmmo - use, 0);
        return type;
    }

    @Override
    public float getAmmoFraction() {
        FactoryGroup g = GroupManager.getGroup(this);
        if (g == null) return super.getAmmoFraction();
        if (ammo.size == 0) return 0f;
        Item cur = getEntryItem(ammo.peek());
        if (cur == null) return 0f;
        items = g.sharedItems;
        return Math.min(1f, (float) items.get(cur) / Math.max(1, maxAmmo));
    }

    /** 炮塔不主动 dump 弹药 */
    @Override
    public boolean dump(Item item) {
        if (GroupManager.getGroup(this) != null) return false;
        return super.dump(item);
    }

    // ===== 液体 =====

    @Override
    public boolean acceptLiquid(Building source, Liquid liquid) {
        if (GroupManager.getGroup(this) != null) {
            return GroupSupport.acceptLiquid(this, source, liquid);
        }
        return super.acceptLiquid(source, liquid);
    }

    @Override
    public void dumpLiquid(Liquid liquid, float scaling, int outputDir) {
        if (GroupManager.getGroup(this) != null) {
            GroupSupport.dumpLiquidFiltered(this, liquid, scaling, outputDir);
        } else {
            super.dumpLiquid(liquid, scaling, outputDir);
        }
    }

    // ===== 电力 =====

    @Override
    public Seq<Building> getPowerConnections(Seq<Building> out) {
        super.getPowerConnections(out);
        return GroupSupport.getPowerConnections(this, out);
    }

    // ===== UI =====

    @Override
    public void display(Table table) {
        super.display(table);
        GroupSupport.displayGroup(this, table);
    }
}