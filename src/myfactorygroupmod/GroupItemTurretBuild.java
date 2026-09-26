package myfactorygroupmod;

import arc.graphics.Color;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Log;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Building;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.blocks.defense.turrets.Turret;

public class GroupItemTurretBuild extends ItemTurret.ItemTurretBuild {

    /** 玩家选择的弹药；null = 自动从共享池挑 */
    public Item selectedAmmo;

    private Item lastSyncedItem;
    private int lastSyncedAmount = -1;

    // === 反射缓存 ===
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

    private Turret.AmmoEntry makeEntry(Item item, int amount) {
        if (itemEntryCtor == null) return null;
        try {
            return (Turret.AmmoEntry) itemEntryCtor.newInstance((ItemTurret) block, item, amount);
        } catch (Throwable t) {
            return null;
        }
    }

    private Item getEntryItem(Turret.AmmoEntry entry) {
        if (entry == null || itemEntryItemField == null) return null;
        try {
            return (Item) itemEntryItemField.get(entry);
        } catch (Throwable t) {
            return null;
        }
    }

    /** 判断物品是否是群内任何炮塔的弹药类型 */
    private boolean isGroupAmmoType(FactoryGroup g, Item item) {
        for (Building b : g.members) {
            if (b.block instanceof ItemTurret it && it.ammoTypes.get(item) != null) {
                return true;
            }
        }
        return false;
    }

    /** 决定当前该用哪种弹药 */
    private Item resolveAmmo(FactoryGroup g) {
        ItemTurret turret = (ItemTurret) block;

        // 1. 玩家选的
        if (selectedAmmo != null
                && turret.ammoTypes.get(selectedAmmo) != null
                && items.get(selectedAmmo) > 0) {
            return selectedAmmo;
        }

        // 2. 保留当前 ammo 队列的
        if (ammo.size > 0) {
            Item cur = getEntryItem(ammo.peek());
            if (cur != null && turret.ammoTypes.get(cur) != null && items.get(cur) > 0) {
                return cur;
            }
        }

        // 3. 共享池里任选一个自己能用的
        for (Item it : turret.ammoTypes.keys()) {
            if (items.get(it) > 0) return it;
        }
        return null;
    }

    private void syncAmmo(FactoryGroup g) {
        items = g.sharedItems;
        Item target = resolveAmmo(g);
        int amount = target == null ? 0 : items.get(target);

        if (target == lastSyncedItem && amount == lastSyncedAmount) return;

        ammo.clear();
        int maxA = ((ItemTurret) block).maxAmmo;
        if (target != null) {
            Turret.AmmoEntry e = makeEntry(target, Math.min(amount, maxA));
            if (e != null) ammo.add(e);
            totalAmmo = Math.min(amount, maxA);
        } else {
            totalAmmo = 0;
        }

        lastSyncedItem = target;
        lastSyncedAmount = amount;
    }

    @Override
    public void updateTile() {
        GroupSupport.redirectModules(this);
        FactoryGroup g = GroupManager.getGroup(this);
        if (g != null) syncAmmo(g);
        super.updateTile();
    }

    // ===== 弹药判定 =====

    @Override
    public boolean hasAmmo() {
        FactoryGroup g = GroupManager.getGroup(this);
        if (g == null) return super.hasAmmo();
        if (!canConsume()) return false;
        if (cheating()) return true;
        items = g.sharedItems;
        Item target = resolveAmmo(g);
        if (target == null) return false;
        return items.get(target) >= ((ItemTurret) block).ammoPerShot;
    }

    @Override
    public BulletType useAmmo() {
        FactoryGroup g = GroupManager.getGroup(this);
        if (g == null) return super.useAmmo();
        if (cheating()) return peekAmmo();

        items = g.sharedItems;
        Item target = resolveAmmo(g);
        if (target == null) return null;

        int use = ((ItemTurret) block).ammoPerShot;
        if (items.get(target) < use) return null;

        items.remove(target, use);
        lastSyncedAmount = -1;
        return ((ItemTurret) block).ammoTypes.get(target);
    }

    @Override
    public BulletType peekAmmo() {
        FactoryGroup g = GroupManager.getGroup(this);
        if (g == null) return super.peekAmmo();
        items = g.sharedItems;
        Item target = resolveAmmo(g);
        return target == null ? null : ((ItemTurret) block).ammoTypes.get(target);
    }

    @Override
    public float getAmmoFraction() {
        FactoryGroup g = GroupManager.getGroup(this);
        if (g == null) return super.getAmmoFraction();
        items = g.sharedItems;
        Item target = resolveAmmo(g);
        if (target == null) return 0f;
        int maxA = ((ItemTurret) block).maxAmmo;
        return Math.min(1f, (float) items.get(target) / Math.max(1, maxA));
    }

    // ===== 物品接收：白名单改为群内所有炮塔的弹药 ====

    @Override
    public boolean acceptItem(Building source, Item item) {
        FactoryGroup g = GroupManager.getGroup(this);
        if (g == null) return super.acceptItem(source, item);
        // 群内任何一个炮塔的弹药类型都接受
        if (!isGroupAmmoType(g, item)) return false;
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
        // 不再拒绝非自己弹药类型：进共享池就行
        items = g.sharedItems;
        items.add(item, 1);
        lastSyncedAmount = -1;
    }

    /** 炮塔不主动 dump 弹药 */
    @Override
    public boolean dump(Item item) {
        if (GroupManager.getGroup(this) != null) return false;
        return super.dump(item);
    }

    // ===== 弹药选择 UI =====

    @Override
    public void buildConfiguration(Table table) {
        if (GroupManager.getGroup(this) == null) {
            super.buildConfiguration(table);
            return;
        }
        ItemTurret turret = (ItemTurret) block;

        table.table(t -> {
            t.defaults().size(48f).pad(4f);
            int i = 0;
            int cols = turret.selectionColumns > 0 ? turret.selectionColumns : 4;
            for (Item it : turret.ammoTypes.keys()) {
                if (i % cols == 0) t.row();
                final Item item = it;
                t.button(new TextureRegionDrawable(it.uiIcon), () -> {
                    selectedAmmo = (selectedAmmo == item) ? null : item;
                    lastSyncedAmount = -1;
                    deselect();
                }).update(b -> {
                    b.getImage().setColor(selectedAmmo == item ? Pal.accent : Color.white);
                }).tooltip(it.localizedName);
                i++;
            }
        });
    }

    @Override
    public boolean shouldShowConfigure(mindustry.gen.Player player) {
        return true;
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