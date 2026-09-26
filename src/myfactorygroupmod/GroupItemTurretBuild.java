package myfactorygroupmod;

import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.blocks.defense.turrets.Turret;

public class GroupItemTurretBuild extends ItemTurret.ItemTurretBuild {

    public GroupItemTurretBuild(ItemTurret turret) {
        turret.super();
    }

    /** 自定义 AmmoEntry，替代原版 package-private 的 ItemTurret.ItemEntry */
    public static class GroupAmmoEntry extends Turret.AmmoEntry {
        public Item item;
        public final ObjectMap<Item, BulletType> ammoTypes;

        public GroupAmmoEntry(Item item, ObjectMap<Item, BulletType> ammoTypes) {
            this.item = item;
            this.ammoTypes = ammoTypes;
        }

        @Override
        public BulletType type() {
            return ammoTypes.get(item);
        }
    }

    @Override
    public void updateTile() {
        GroupSupport.redirectModules(this);
        super.updateTile();
    }

    /** 从 ammo 队列中读取当前"选中"的弹药类型 */
    private Item currentAmmoItem() {
        if (ammo.size == 0) return null;
        Turret.AmmoEntry entry = ammo.peek();
        if (entry instanceof GroupAmmoEntry g) return g.item;
        if (entry instanceof ItemTurret.ItemEntry ie) return ie.item;
        return null;
    }

    // ===== 弹药共享 =====

    @Override
    public boolean acceptItem(Building source, Item item) {
        FactoryGroup g = GroupManager.getGroup(this);
        if (g == null) return super.acceptItem(source, item);

        // 只接受有效弹药类型
        if (((ItemTurret) block).ammoTypes.get(item) == null) return false;

        items = g.sharedItems;
        return true;
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
        totalAmmo += type.ammoMultiplier;

        // 更新 ammo 队列：把该物品移到队尾（作为当前"选中"类型）
        for (int i = 0; i < ammo.size; i++) {
            Turret.AmmoEntry entry = ammo.get(i);
            Item entryItem = null;
            if (entry instanceof GroupAmmoEntry ga) entryItem = ga.item;
            else if (entry instanceof ItemTurret.ItemEntry ie) entryItem = ie.item;

            if (entryItem == item) {
                ammo.swap(i, ammo.size - 1);
                return;
            }
        }
        ammo.add(new GroupAmmoEntry(item, turret.ammoTypes));
    }

    @Override
    public boolean hasAmmo() {
        FactoryGroup g = GroupManager.getGroup(this);
        if (g == null) return super.hasAmmo();
        if (!canConsume()) return false;
        Item cur = currentAmmoItem();
        if (cur == null) return false;
        items = g.sharedItems;
        return items.get(cur) >= ((ItemTurret) block).ammoPerShot || cheating();
    }

    @Override
    public BulletType useAmmo() {
        FactoryGroup g = GroupManager.getGroup(this);
        if (g == null) return super.useAmmo();
        if (cheating()) return peekAmmo();
        Item cur = currentAmmoItem();
        if (cur == null) return null;
        items = g.sharedItems;
        int use = ((ItemTurret) block).ammoPerShot;
        items.remove(cur, use);
        totalAmmo = Math.max(totalAmmo - use, 0);
        return ((ItemTurret) block).ammoTypes.get(cur);
    }

    @Override
    public BulletType peekAmmo() {
        FactoryGroup g = GroupManager.getGroup(this);
        if (g == null) return super.peekAmmo();
        Item cur = currentAmmoItem();
        return cur == null ? null : ((ItemTurret) block).ammoTypes.get(cur);
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